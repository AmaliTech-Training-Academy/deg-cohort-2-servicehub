import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DashboardService, ServiceRequestResponse } from '../../../core/services/dashboard.service';
import { SlaBadge } from '../../../shared/components/sla-badge/sla-badge';

@Component({
  selector: 'app-agent-dashboard',
  imports: [FormsModule, SlaBadge],
  templateUrl: './agent-dashboard.html',
})
export class AgentDashboard implements OnInit {
  private dashboardService = inject(DashboardService);

  requests: ServiceRequestResponse[] = [];
  loading = true;
  error = '';
  advancing = new Set<number>();

  q = '';
  statusFilter = 'ALL';
  prioFilter = 'ALL';

  readonly NEXT_STATUS: Record<string, string> = {
    OPEN: 'ASSIGNED', ASSIGNED: 'IN_PROGRESS', IN_PROGRESS: 'RESOLVED',
  };
  readonly NEXT_LABEL: Record<string, string> = {
    OPEN: 'Assign', ASSIGNED: 'Start', IN_PROGRESS: 'Resolve',
  };
  readonly CAT_LABEL: Record<string, string> = {
    IT_SUPPORT: 'IT Support', FACILITIES: 'Facilities', HR_REQUEST: 'HR Request',
  };
  readonly CAT_ICON: Record<string, string> = {
    IT_SUPPORT: 'M5 6h14v9H5zM3 19h18l-1-2H4l-1 2Z',
    FACILITIES: 'M14.5 6.5a3.5 3.5 0 0 0-4.6 4.3L4 16.7 7.3 20l5.9-5.9a3.5 3.5 0 0 0 4.3-4.6l-2.3 2.3-2-2 2.3-2.3Z',
    HR_REQUEST: 'M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-5ZM20 13h-2a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-5ZM18 19a3 3 0 0 1-3 3h-3',
  };
  readonly STATUS_LABEL: Record<string, string> = {
    OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved', CLOSED: 'Closed',
  };

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading = true;
    this.dashboardService.getRequests(0, 20).subscribe({
      next: p => { this.requests = p.content; this.loading = false; },
      error: () => { this.error = 'Failed to load tickets.'; this.loading = false; },
    });
  }

  get filtered(): ServiceRequestResponse[] {
    return this.requests.filter(r => {
      if (this.statusFilter !== 'ALL' && r.status !== this.statusFilter) return false;
      if (this.prioFilter !== 'ALL' && r.priority !== this.prioFilter) return false;
      if (this.q.trim()) {
        const hay = (`${r.id} ${r.title} ${r.requesterName}`).toLowerCase();
        if (!hay.includes(this.q.trim().toLowerCase())) return false;
      }
      return true;
    });
  }

  get isDirty(): boolean { return !!this.q.trim() || this.statusFilter !== 'ALL' || this.prioFilter !== 'ALL'; }

  clearFilters(): void { this.q = ''; this.statusFilter = 'ALL'; this.prioFilter = 'ALL'; }

  nextStatus(r: ServiceRequestResponse): string | null { return this.NEXT_STATUS[r.status] ?? null; }
  nextLabel(r: ServiceRequestResponse): string { return this.NEXT_LABEL[r.status] ?? ''; }

  advance(r: ServiceRequestResponse): void {
    const ns = this.nextStatus(r);
    if (!ns || this.advancing.has(r.id)) return;
    this.advancing.add(r.id);
    this.dashboardService.updateStatus(r.id, ns).subscribe({
      next: updated => {
        const idx = this.requests.findIndex(x => x.id === updated.id);
        if (idx >= 0) this.requests[idx] = updated;
        this.advancing.delete(r.id);
      },
      error: () => this.advancing.delete(r.id),
    });
  }

  ago(iso: string): string {
    const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
    if (mins < 60) return mins + 'm ago';
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
  }

  ticketId(id: number): string { return 'SH-' + id; }
}
