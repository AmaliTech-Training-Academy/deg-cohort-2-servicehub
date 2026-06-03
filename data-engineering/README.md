# ServiceHub — Data Engineering Pipeline

## What it does

Reads service request data from the ServiceHub PostgreSQL database and produces 5 analytics tables, each also exported as a CSV to `output/`.

| Table | What it contains |
|-------|-----------------|
| `analytics_sla_metrics` | Avg and max resolution hours per category + priority |
| `analytics_daily_volume` | Daily request count by category |
| `analytics_sla_breaches` | Active overdue requests and historically breached resolutions |
| `analytics_agent_performance` | Per-agent throughput, avg resolution time, response SLA compliance |
| `analytics_department_workload` | Request volumes by department, status, and ISO week |

## Prerequisites

- Python 3.10+
- PostgreSQL running with the ServiceHub database (V6 migration applied)

## Setup

```bash
cd data-engineering
pip install -r requirements.txt
```

## Configuration

Create a `.env` file in `data-engineering/` with your database credentials:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=servicehub
DB_USER=postgres
DB_PASSWORD=postgres
```

Or export `DATABASE_URL` directly:

```bash
export DATABASE_URL=postgresql://postgres:postgres@localhost:5432/servicehub
```

## Run

```bash
python etl_pipeline.py
```

Expected output:

```
Starting ServiceHub ETL pipeline...
Extracted 55 requests, 12 SLA policies
Loaded 12 rows into analytics_sla_metrics (CSV: output/analytics_sla_metrics.csv)
Loaded N rows into analytics_daily_volume (CSV: output/analytics_daily_volume.csv)
Loaded 17 rows into analytics_sla_breaches (CSV: output/analytics_sla_breaches.csv)
Loaded N rows into analytics_agent_performance (CSV: output/analytics_agent_performance.csv)
Loaded N rows into analytics_department_workload (CSV: output/analytics_department_workload.csv)
ETL pipeline complete!
```

CSV files are saved to `output/` and are not committed to the repo.

> **Demo note:** Requires a running PostgreSQL instance with the V6 sample data migration applied (55 seeded service requests). Run the pipeline locally against the dev database to generate the output files before the demo.
