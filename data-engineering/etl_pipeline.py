"""ETL Pipeline for ServiceHub - SLA Analytics & Resolution Metrics"""
import pandas as pd
from sqlalchemy import create_engine, text
from config import DATABASE_URL

engine = create_engine(DATABASE_URL)

def extract_requests():
    query = text("""
        SELECT sr.id, sr.title, sr.category, sr.priority, sr.status,
               sr.created_at, sr.updated_at, sr.resolved_at,
               sr.sla_deadline, sr.response_deadline, sr.first_response_at,
               sr.assigned_to_id,
               u.full_name AS requester_name, d.name AS department_name
        FROM service_requests sr
        JOIN users u ON sr.requester_id = u.id
        LEFT JOIN departments d ON sr.department_id = d.id
    """)
    with engine.connect() as conn:
        return pd.read_sql(query, conn)

def extract_sla_policies():
    query = text("SELECT * FROM sla_policies")
    with engine.connect() as conn:
        return pd.read_sql(query, conn)

def transform_sla_metrics(requests_df, sla_df):
    """Calculate SLA compliance metrics."""
    if requests_df.empty:
        return pd.DataFrame()
    requests_df["created_at"] = pd.to_datetime(requests_df["created_at"])
    requests_df["resolved_at"] = pd.to_datetime(requests_df["resolved_at"])
    resolved = requests_df[requests_df["resolved_at"].notna()].copy()
    if resolved.empty:
        return pd.DataFrame()
    resolved["resolution_hours"] = (resolved["resolved_at"] - resolved["created_at"]).dt.total_seconds() / 3600

    summary = resolved.groupby(["category", "priority"]).agg(
        total_resolved=("id", "count"),
        avg_resolution_hours=("resolution_hours", "mean"),
        max_resolution_hours=("resolution_hours", "max"),
    ).reset_index()
    return summary

def transform_daily_volume(requests_df):
    """Daily request volume by category."""
    if requests_df.empty:
        return pd.DataFrame()
    requests_df["date"] = pd.to_datetime(requests_df["created_at"]).dt.date
    return requests_df.groupby(["date", "category"]).size().reset_index(name="request_count")

def transform_sla_breaches(requests_df):
    """Identify SLA resolution breaches — real-time overdue and historical.

    breach_type RESOLUTION_OVERDUE: active request past its sla_deadline.
    breach_type RESOLUTION_BREACHED: resolved/closed request where resolved_at > sla_deadline.
    breach_hours: how far past the deadline (positive = breach magnitude).
    Aligns with the backend /api/requests/overdue slaStatus contract.
    """
    if requests_df.empty:
        return pd.DataFrame()

    df = requests_df.copy()
    df["sla_deadline"] = pd.to_datetime(df["sla_deadline"], utc=True)
    df["resolved_at"] = pd.to_datetime(df["resolved_at"], utc=True)
    now = pd.Timestamp.now(tz="UTC")

    active = df[~df["status"].isin(["RESOLVED", "CLOSED"])].copy()
    overdue = active[active["sla_deadline"].notna() & (active["sla_deadline"] < now)].copy()
    overdue["breach_type"] = "RESOLUTION_OVERDUE"
    overdue["breach_hours"] = (now - overdue["sla_deadline"]).dt.total_seconds() / 3600

    resolved = df[df["status"].isin(["RESOLVED", "CLOSED"])].copy()
    historical = resolved[
        resolved["resolved_at"].notna() &
        resolved["sla_deadline"].notna() &
        (resolved["resolved_at"] > resolved["sla_deadline"])
    ].copy()
    historical["breach_type"] = "RESOLUTION_BREACHED"
    historical["breach_hours"] = (historical["resolved_at"] - historical["sla_deadline"]).dt.total_seconds() / 3600

    cols = ["id", "category", "priority", "status", "breach_type", "breach_hours"]
    result = pd.concat([overdue[cols], historical[cols]], ignore_index=True)
    return result


def transform_agent_performance(requests_df):
    """Per-agent resolution and response time metrics.

    Groups by assigned_to_id. Computes resolution throughput, average hours,
    and response SLA compliance rate (first_response_at <= response_deadline).
    Requests with no assigned_to_id (OPEN, unassigned) are excluded.
    """
    if requests_df.empty:
        return pd.DataFrame()

    df = requests_df[requests_df["assigned_to_id"].notna()].copy()
    if df.empty:
        return pd.DataFrame()

    df["created_at"] = pd.to_datetime(df["created_at"], utc=True)
    df["resolved_at"] = pd.to_datetime(df["resolved_at"], utc=True)
    df["first_response_at"] = pd.to_datetime(df["first_response_at"], utc=True)
    df["response_deadline"] = pd.to_datetime(df["response_deadline"], utc=True)

    resolved = df[df["resolved_at"].notna()].copy()
    resolved["resolution_hours"] = (
        resolved["resolved_at"] - resolved["created_at"]
    ).dt.total_seconds() / 3600

    responded = df[df["first_response_at"].notna()].copy()
    responded["response_hours"] = (
        responded["first_response_at"] - responded["created_at"]
    ).dt.total_seconds() / 3600
    responded["response_sla_met"] = responded["first_response_at"] <= responded["response_deadline"]

    total = df.groupby("assigned_to_id").agg(
        total_assigned=("id", "count")
    ).reset_index()

    res_stats = resolved.groupby("assigned_to_id").agg(
        total_resolved=("id", "count"),
        avg_resolution_hours=("resolution_hours", "mean"),
        max_resolution_hours=("resolution_hours", "max"),
    ).reset_index()

    resp_stats = responded.groupby("assigned_to_id").agg(
        avg_response_hours=("response_hours", "mean"),
        response_sla_compliance_rate=("response_sla_met", "mean"),
    ).reset_index()

    result = total.merge(res_stats, on="assigned_to_id", how="left")
    result = result.merge(resp_stats, on="assigned_to_id", how="left")
    return result


def transform_department_workload(requests_df):
    """Department request volumes by status and ISO week.

    Returns one row per (department_name, status, week) combination.
    week is the ISO year-week string (e.g. '2026-W22') derived from created_at.
    Provides both a current status snapshot and a historical weekly trend.
    """
    if requests_df.empty:
        return pd.DataFrame()

    df = requests_df.copy()
    df["created_at"] = pd.to_datetime(df["created_at"], utc=True)
    df["week"] = df["created_at"].dt.strftime("%G-W%V")

    result = (
        df.groupby(["department_name", "status", "week"])
        .size()
        .reset_index(name="request_count")
    )
    return result


def load_analytics(df, table_name):
    df.to_sql(table_name, engine, if_exists="replace", index=False)
    print(f"Loaded {len(df)} rows into {table_name}")

def run_pipeline():
    print("Starting ServiceHub ETL pipeline...")
    requests_df = extract_requests()
    sla_df = extract_sla_policies()
    print(f"Extracted {len(requests_df)} requests, {len(sla_df)} SLA policies")

    sla_metrics = transform_sla_metrics(requests_df, sla_df)
    if not sla_metrics.empty:
        load_analytics(sla_metrics, "analytics_sla_metrics")

    daily_volume = transform_daily_volume(requests_df)
    if not daily_volume.empty:
        load_analytics(daily_volume, "analytics_daily_volume")

    sla_breaches = transform_sla_breaches(requests_df)
    if not sla_breaches.empty:
        load_analytics(sla_breaches, "analytics_sla_breaches")

    agent_perf = transform_agent_performance(requests_df)
    if not agent_perf.empty:
        load_analytics(agent_perf, "analytics_agent_performance")

    dept_workload = transform_department_workload(requests_df)
    if not dept_workload.empty:
        load_analytics(dept_workload, "analytics_department_workload")

    print("ETL pipeline complete!")

if __name__ == "__main__":
    run_pipeline()
