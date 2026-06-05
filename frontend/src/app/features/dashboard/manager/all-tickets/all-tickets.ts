import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DashboardService, ServiceRequestResponse } from '../../../../core/services/dashboard.service';
import { PriorityChipComponent } from '../../../../shared/components/priority-chip/priority-chip';
import { StatusDotComponent } from '../../../../shared/components/status-dot/status-dot';
import { SlaTagComponent } from '../../../../shared/components/sla-tag/sla-tag';

@Component({
  selector: 'app-all-tickets',
  imports: [FormsModule, PriorityChipComponent, StatusDotComponent, SlaTagComponent],
  templateUrl: './all-tickets.html',
})
export class AllTicketsComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly requests = signal<ServiceRequestResponse[]>([]);
  readonly q = signal('');
  readonly statusFilter = signal('ALL');
  readonly prioFilter = signal('ALL');
  readonly catFilter = signal('ALL');

  readonly CAT_LABEL: Partial<Record<string, string>> = {
    IT_SUPPORT: 'IT Support', FACILITIES: 'Facilities', HR_REQUEST: 'HR Request',
  };
  readonly CAT_ICON: Record<string, string> = {
    IT_SUPPORT: 'M5 6h14v9H5zM3 19h18l-1-2H4l-1 2Z',
    FACILITIES: 'M14.5 6.5a3.5 3.5 0 0 0-4.6 4.3L4 16.7 7.3 20l5.9-5.9a3.5 3.5 0 0 0 4.3-4.6l-2.3 2.3-2-2 2.3-2.3Z',
    HR_REQUEST: 'M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-5ZM20 13h-2a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-5ZM18 19a3 3 0 0 1-3 3h-3',
  };

  readonly filtered = computed(() => {
    const q = this.q().trim().toLowerCase();
    return this.requests().filter(r => {
      if (this.statusFilter() !== 'ALL' && r.status !== this.statusFilter()) return false;
      if (this.prioFilter() !== 'ALL' && r.priority !== this.prioFilter()) return false;
      if (this.catFilter() !== 'ALL' && r.category !== this.catFilter()) return false;
      if (q) {
        const hay = `${r.id} ${r.title} ${r.requesterName}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly isDirty = computed(() =>
    !!this.q().trim() || this.statusFilter() !== 'ALL' || this.prioFilter() !== 'ALL' || this.catFilter() !== 'ALL'
  );

  ngOnInit(): void {
    this.dashboardService.getRequests(0, 100).subscribe({
      next: p => { this.requests.set(p.content); this.loading.set(false); },
      error: () => { this.error.set('Failed to load tickets.'); this.loading.set(false); },
    });
  }

  clearFilters(): void { this.q.set(''); this.statusFilter.set('ALL'); this.prioFilter.set('ALL'); this.catFilter.set('ALL'); }
  ago(iso: string): string {
    const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
    if (mins < 60) return mins + 'm ago';
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
  }
  ticketId(id: number): string { return 'SH-' + id; }
}
