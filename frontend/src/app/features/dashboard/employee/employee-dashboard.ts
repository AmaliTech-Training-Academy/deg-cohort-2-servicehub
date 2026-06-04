import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardService, ServiceRequestResponse } from '../../../core/services/dashboard.service';
import { SseService, SseEvent } from '../../../core/services/sse.service';
import { PriorityChipComponent } from '../../../shared/components/priority-chip/priority-chip';
import { StatusDotComponent } from '../../../shared/components/status-dot/status-dot';
import { SlaTagComponent } from '../../../shared/components/sla-tag/sla-tag';
import { TicketDetailComponent } from '../agent/ticket-detail/ticket-detail';

@Component({
  selector: 'app-employee-dashboard',
  imports: [FormsModule, PriorityChipComponent, StatusDotComponent, SlaTagComponent, TicketDetailComponent],
  templateUrl: './employee-dashboard.html',
})
export class EmployeeDashboard implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private sseService = inject(SseService);
  private router = inject(Router);

  readonly requests = signal<ServiceRequestResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly selectedRequest = signal<ServiceRequestResponse | null>(null);
  readonly toastMessage = signal('');

  readonly statusFilter = signal('ALL');
  readonly prioFilter = signal('ALL');

  readonly CAT_LABEL: Partial<Record<string, string>> = {
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

  private sseSubscription?: Subscription;
  private toastTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.load();
    this.connectToStream();
  }

  ngOnDestroy(): void {
    this.sseSubscription?.unsubscribe();
    clearTimeout(this.toastTimer);
  }

  private load(): void {
    this.dashboardService.getMyRequests(0, 20).subscribe({
      next: p => { this.requests.set(p.content); this.loading.set(false); },
      error: () => { this.error.set('Failed to load your requests.'); this.loading.set(false); },
    });
  }

  private refresh(): void {
    this.dashboardService.getMyRequests(0, 20).subscribe({
      next: p => this.requests.set(p.content),
      error: () => { /* silent — existing list stays visible on refresh failure */ },
    });
  }

  private connectToStream(): void {
    const token = this.authService.getToken();
    if (!token) return;

    const url = `${environment.apiUrl}/api/notifications/stream?token=${token}`;

    this.sseSubscription = this.sseService.stream<SseEvent>(url).subscribe({
      next: event => {
        if (event.type === 'TICKET_UPDATED' || event.type === 'SLA_BREACHED') {
          this.refresh();
          const msg = event.type === 'SLA_BREACHED'
            ? `Ticket #SH-${event.requestId} SLA breached — priority escalated`
            : `Ticket #SH-${event.requestId} updated to ${this.formatStatus(event.detail)}`;
          this.showToast(msg);
        }
      },
      error: () => { /* SSE errors are handled by EventSource auto-reconnect */ },
    });
  }

  private formatStatus(status: string | undefined): string {
    if (!status) return '';
    const labels: Record<string, string> = {
      OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress',
      RESOLVED: 'Resolved', CLOSED: 'Closed',
    };
    return labels[status] ?? status;
  }

  private showToast(message: string): void {
    this.toastMessage.set(message);
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => this.toastMessage.set(''), 3000);
  }

  openDetail(id: number): void {
    const r = this.requests().find(x => x.id === id);
    if (r) this.selectedRequest.set(r);
  }

  closeDetail(): void { this.selectedRequest.set(null); }
  clearFilters(): void { this.statusFilter.set('ALL'); this.prioFilter.set('ALL'); }

  ago(iso: string): string {
    const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
    if (mins < 60) return mins + 'm ago';
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
  }

  ticketId(id: number): string { return 'SH-' + id; }
  goSubmit(): void { this.router.navigateByUrl('/requests/submit'); }
}
