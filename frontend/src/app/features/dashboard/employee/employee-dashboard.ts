import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DashboardService, ServiceRequestResponse } from '../../../core/services/dashboard.service';
import { SlaBadge } from '../../../shared/components/sla-badge/sla-badge';

@Component({
  selector: 'app-employee-dashboard',
  imports: [FormsModule, SlaBadge],
  templateUrl: './employee-dashboard.html',
})
export class EmployeeDashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly requests = signal<ServiceRequestResponse[]>([]);

  statusFilter = signal('ALL');
  prioFilter = signal('ALL');

  readonly STATUS_LABEL: Record<string, string> = {
    OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved', CLOSED: 'Closed',
  };
  readonly CAT_LABEL: Record<string, string> = {
    IT_SUPPORT: 'IT Support', FACILITIES: 'Facilities', HR_REQUEST: 'HR Request',
  };

  readonly filtered = computed(() =>
    this.requests().filter(r => {
      if (this.statusFilter() !== 'ALL' && r.status !== this.statusFilter()) return false;
      if (this.prioFilter() !== 'ALL' && r.priority !== this.prioFilter()) return false;
      return true;
    })
  );

  readonly isDirty = computed(() => this.statusFilter() !== 'ALL' || this.prioFilter() !== 'ALL');

  ngOnInit(): void {
    this.dashboardService.getMyRequests(0, 20).subscribe({
      next: p => { this.requests.set(p.content); this.loading.set(false); },
      error: () => { this.error.set('Failed to load your requests.'); this.loading.set(false); },
    });
  }

  clearFilters(): void { this.statusFilter.set('ALL'); this.prioFilter.set('ALL'); }

  ago(iso: string): string {
    const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
    if (mins < 60) return mins + 'm ago';
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
  }

  ticketId(id: number): string { return 'SH-' + id; }
  goSubmit(): void { this.router.navigateByUrl('/submit-request'); }
}
