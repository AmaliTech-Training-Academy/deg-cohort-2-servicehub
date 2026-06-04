import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardService, ServiceRequestResponse } from '../../../core/services/dashboard.service';
import { SseService, SseEvent } from '../../../core/services/sse.service';
import { formatStatus } from '../../../shared/utils/status-labels';
import { PriorityChipComponent } from '../../../shared/components/priority-chip/priority-chip';
import { StatusDotComponent } from '../../../shared/components/status-dot/status-dot';
import { SlaTagComponent } from '../../../shared/components/sla-tag/sla-tag';
import { TicketDetailComponent } from './ticket-detail/ticket-detail';

@Component({
  selector: 'app-agent-dashboard',
  imports: [FormsModule, PriorityChipComponent, StatusDotComponent, SlaTagComponent, TicketDetailComponent],
  templateUrl: './agent-dashboard.html',
})
export class AgentDashboard implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private sseService = inject(SseService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly requests = signal<ServiceRequestResponse[]>([]);
  readonly advancing = signal(new Set<number>());
  readonly selectedRequest = signal<ServiceRequestResponse | null>(null);
  readonly toastMessage = signal('');
  readonly connectionStatus = signal<'connected' | 'reconnecting'>('connected');

  readonly q = signal('');
  readonly statusFilter = signal('ALL');
  readonly prioFilter = signal('ALL');

  readonly NEXT_STATUS: Record<string, string> = {
    OPEN: 'ASSIGNED', ASSIGNED: 'IN_PROGRESS', IN_PROGRESS: 'RESOLVED', RESOLVED: 'CLOSED',
  };
  readonly NEXT_LABEL: Record<string, string> = {
    OPEN: 'Assign', ASSIGNED: 'Start', IN_PROGRESS: 'Resolve', RESOLVED: 'Close',
  };
  readonly CAT_LABEL: Record<string, string> = {
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
      if (q) {
        const hay = `${r.id} ${r.title} ${r.requesterName}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  readonly isDirty = computed(() =>
    !!this.q().trim() || this.statusFilter() !== 'ALL' || this.prioFilter() !== 'ALL'
  );

  private readonly sseSubscription = new Subscription();
  private readonly refreshTrigger = new Subject<void>();
  private toastTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.load();

    this.sseSubscription.add(
      this.refreshTrigger.pipe(
        debounceTime(200),
        switchMap(() => this.dashboardService.getRequests(0, 20))
      ).subscribe({
        next: p => this.requests.set(p.content),
        error: () => { /* silent — existing list stays visible on refresh failure */ },
      })
    );

    this.connectToStream();
  }

  ngOnDestroy(): void {
    this.sseSubscription.unsubscribe();
    clearTimeout(this.toastTimer);
  }

  private load(): void {
    this.loading.set(true);
    this.dashboardService.getRequests(0, 20).subscribe({
      next: p => { this.requests.set(p.content); this.loading.set(false); },
      error: () => { this.error.set('Failed to load tickets.'); this.loading.set(false); },
    });
  }

  private refresh(): void { this.refreshTrigger.next(); }

  private connectToStream(): void {
    const token = this.authService.getToken();
    if (!token) return;

    // Security note: EventSource does not support custom headers, so the JWT must be
    // passed as a query parameter. This means the token appears in server access logs
    // and the browser Network tab URL. This is an accepted trade-off for SSE — the
    // backend validates the token before creating the emitter and rejects invalid ones.
    const url = `${environment.apiUrl}/api/notifications/stream?token=${encodeURIComponent(token)}`;

    this.sseSubscription.add(
      this.sseService.stream<SseEvent>(url, () => this.connectionStatus.set('reconnecting')).subscribe({
        next: event => {
          this.connectionStatus.set('connected');
          if (event.type === 'TICKET_UPDATED' || event.type === 'TICKET_ASSIGNED') {
            this.refresh();
            this.showToast(`Ticket #SH-${event.requestId} updated to ${formatStatus(event.detail)}`);
          }
          if (event.type === 'SLA_BREACHED') {
            this.refresh();
            this.showToast(`Ticket #SH-${event.requestId} SLA breached — priority escalated`);
          }
        },
        error: () => { /* SSE errors are handled by EventSource auto-reconnect */ },
      })
    );
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

  onDetailAdvanced(updated: ServiceRequestResponse): void {
    this.requests.update(list => list.map(r => r.id === updated.id ? updated : r));
    this.selectedRequest.set(updated);
  }

  clearFilters(): void { this.q.set(''); this.statusFilter.set('ALL'); this.prioFilter.set('ALL'); }

  nextStatus(r: ServiceRequestResponse): string | null { return this.NEXT_STATUS[r.status] ?? null; }
  nextLabel(r: ServiceRequestResponse): string { return this.NEXT_LABEL[r.status] ?? ''; }
  isAdvancing(id: number): boolean { return this.advancing().has(id); }

  advance(r: ServiceRequestResponse): void {
    const ns = this.nextStatus(r);
    if (!ns || this.isAdvancing(r.id)) return;

    this.advancing.update(s => new Set([...s, r.id]));
    this.dashboardService.updateStatus(r.id, ns).subscribe({
      next: updated => {
        this.requests.update(list => list.map(x => x.id === updated.id ? updated : x));
        this.advancing.update(s => { const n = new Set(s); n.delete(r.id); return n; });
      },
      error: () => {
        this.advancing.update(s => { const n = new Set(s); n.delete(r.id); return n; });
      },
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
