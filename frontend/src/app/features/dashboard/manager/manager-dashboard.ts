import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DashboardService, DashboardStatsResponse } from '../../../core/services/dashboard.service';

interface TrendDay { label: string; count: number; }

const EMPTY_STATS: DashboardStatsResponse = {
  totalRequests: 0, openRequests: 0, resolvedRequests: 0,
  avgResolutionHours: 0, slaComplianceRate: 0,
  requestsByCategory: {}, requestsByPriority: {},
  requestsByStatus: {}, slaByCategory: {},
};

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './manager-dashboard.html',
})
export class ManagerDashboard implements OnInit {
  private dashboardService = inject(DashboardService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly stats = signal<DashboardStatsResponse>(EMPTY_STATS);
  readonly trendDays = signal<TrendDay[]>([]);

  readonly STATUS_LABEL: Record<string, string> = {
    OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress',
    RESOLVED: 'Resolved', CLOSED: 'Closed',
  };
  readonly STATUS_COLOR: Record<string, string> = {
    OPEN: '#9A968A', ASSIGNED: 'var(--blue)', IN_PROGRESS: 'var(--amber)',
    RESOLVED: 'var(--teal)', CLOSED: '#B7B2A6',
  };
  readonly CAT_LABEL: Record<string, string> = {
    IT_SUPPORT: 'IT Support', FACILITIES: 'Facilities', HR_REQUEST: 'HR Request',
  };
  readonly CAT_COLOR: Record<string, string> = {
    IT_SUPPORT: 'var(--blue)', FACILITIES: 'var(--amber)', HR_REQUEST: 'var(--teal)',
  };

  ngOnInit(): void {
    forkJoin({
      stats: this.dashboardService.getStats(),
      trends: this.dashboardService.getTrends(7),
    }).subscribe({
      next: ({ stats, trends }) => {
        this.stats.set(stats);
        this.trendDays.set(
          Object.entries(trends)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([date, count]) => ({ label: this.shortDate(date), count }))
        );
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load dashboard data.'); this.loading.set(false); },
    });
  }

  private shortDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { month: 'short', day: 'numeric' });
  }

  /* ── computed ── */
  readonly compliancePct = computed(() => Math.round((this.stats().slaComplianceRate ?? 0) * 100));
  readonly complianceColor = computed(() => {
    const p = this.compliancePct();
    return p >= 80 ? 'var(--teal)' : p >= 60 ? 'var(--amber)' : 'var(--red)';
  });
  readonly donutCirc = 2 * Math.PI * 54;
  readonly donutOffset = computed(() => this.donutCirc * (1 - this.compliancePct() / 100));
  readonly avgDisplay = computed(() => {
    const h = this.stats().avgResolutionHours ?? 0;
    return h > 0 ? h.toFixed(1) + 'h' : '—';
  });
  readonly maxCat = computed(() =>
    Math.max(...Object.values(this.stats().requestsByCategory ?? {}), 1)
  );
  readonly maxStatus = computed(() =>
    Math.max(...Object.values(this.stats().requestsByStatus ?? {}), 1)
  );
  readonly maxTrend = computed(() =>
    Math.max(...this.trendDays().map(d => d.count), 1)
  );

  /* ── helpers called from template ── */
  catWidth(cat: string): string {
    return ((this.stats().requestsByCategory?.[cat] ?? 0) / this.maxCat() * 100) + '%';
  }
  catCount(cat: string): number { return this.stats().requestsByCategory?.[cat] ?? 0; }
  statusWidth(s: string): string {
    return ((this.stats().requestsByStatus?.[s] ?? 0) / this.maxStatus() * 100) + '%';
  }
  statusCount(s: string): number { return this.stats().requestsByStatus?.[s] ?? 0; }
  trendWidth(count: number): string { return (count / this.maxTrend() * 100) + '%'; }
  prioCount(p: string): number { return this.stats().requestsByPriority[p] ?? 0; }
  slaCatPct(cat: string): number {
    return Math.round((this.stats().slaByCategory?.[cat] ?? 0) * 100);
  }
  slaCatColor(cat: string): string {
    const p = this.slaCatPct(cat);
    return p >= 80 ? 'var(--teal)' : p >= 60 ? 'var(--amber)' : 'var(--red)';
  }
}
