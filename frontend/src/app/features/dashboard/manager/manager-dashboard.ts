import { Component, inject, OnInit } from '@angular/core';
import { DashboardService, DashboardStatsResponse } from '../../../core/services/dashboard.service';

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './manager-dashboard.html',
})
export class ManagerDashboard implements OnInit {
  private dashboardService = inject(DashboardService);

  loading = true;
  error = '';
  stats: DashboardStatsResponse = {
    totalRequests: 0, openRequests: 0, resolvedRequests: 0,
    avgResolutionHours: 0, slaComplianceRate: 1,
    requestsByCategory: { IT_SUPPORT: 0, FACILITIES: 0, HR_REQUEST: 0 },
    requestsByPriority: {},
  };

  ngOnInit(): void {
    // TODO: integrate GET /api/dashboard/stats once backend PR is merged
    this.dashboardService.getStats().subscribe({
      next: s => { this.stats = s; this.loading = false; },
      error: () => { this.error = 'Failed to load dashboard stats.'; this.loading = false; },
    });
  }

  get compliancePct(): number { return Math.round(this.stats.slaComplianceRate * 100); }

  get complianceColor(): string {
    const p = this.compliancePct;
    return p >= 80 ? 'var(--teal)' : p >= 60 ? 'var(--amber)' : 'var(--red)';
  }

  get donutOffset(): number {
    const r = 54, circ = 2 * Math.PI * r;
    return circ * (1 - this.compliancePct / 100);
  }

  get donutCirc(): number { return 2 * Math.PI * 54; }

  get maxCat(): number {
    return Math.max(...Object.values(this.stats.requestsByCategory), 1);
  }

  catWidth(cat: string): string {
    return ((this.stats.requestsByCategory[cat] ?? 0) / this.maxCat * 100) + '%';
  }

  catCount(cat: string): number { return this.stats.requestsByCategory[cat] ?? 0; }

  get avgDisplay(): string {
    const h = this.stats.avgResolutionHours;
    return h > 0 ? h.toFixed(1) + 'h' : '—';
  }
}
