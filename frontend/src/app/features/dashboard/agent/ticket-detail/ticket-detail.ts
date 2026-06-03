import { Component, EventEmitter, inject, Input, OnChanges, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DashboardService, ServiceRequestResponse } from '../../../../core/services/dashboard.service';
import { PriorityChipComponent } from '../../../../shared/components/priority-chip/priority-chip';
import { StatusDotComponent } from '../../../../shared/components/status-dot/status-dot';
import { SlaTagComponent } from '../../../../shared/components/sla-tag/sla-tag';

@Component({
  selector: 'app-ticket-detail',
  imports: [FormsModule, PriorityChipComponent, StatusDotComponent, SlaTagComponent],
  templateUrl: './ticket-detail.html',
})
export class TicketDetailComponent implements OnChanges {
  private dashboardService = inject(DashboardService);

  @Input({ required: true }) request!: ServiceRequestResponse;
  @Output() back = new EventEmitter<void>();
  @Output() advanced = new EventEmitter<ServiceRequestResponse>();

  readonly advancing = signal(false);
  readonly comment = signal('');

  readonly STEPS = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  readonly STATUS_LABEL: Record<string, string> = {
    OPEN: 'Open', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress',
    RESOLVED: 'Resolved', CLOSED: 'Closed',
  };

  readonly NEXT_STATUS: Record<string, string> = {
    OPEN: 'ASSIGNED', ASSIGNED: 'IN_PROGRESS', IN_PROGRESS: 'RESOLVED', RESOLVED: 'CLOSED',
  };

  readonly ADVANCE_VERB: Record<string, string> = {
    ASSIGNED: 'Assign', IN_PROGRESS: 'Start', RESOLVED: 'Resolve', CLOSED: 'Close',
  };

  readonly CAT_LABEL: Record<string, string> = {
    IT_SUPPORT: 'IT Support', FACILITIES: 'Facilities', HR_REQUEST: 'HR Request',
  };

  nextStatus = '';
  advanceLabel = '';

  ngOnChanges(): void {
    this.nextStatus = this.NEXT_STATUS[this.request?.status] ?? '';
    this.advanceLabel = this.nextStatus ? this.ADVANCE_VERB[this.nextStatus] : '';
  }

  stepClass(step: string): string {
    const curIdx = this.STEPS.indexOf(this.request.status);
    const stepIdx = this.STEPS.indexOf(step);
    if (stepIdx < curIdx) return 'done';
    if (stepIdx === curIdx) return 'curr';
    return 'pending';
  }

  stepTime(step: string): string {
    switch (step) {
      case 'OPEN':     return this.request.createdAt  ? this.fmtDate(this.request.createdAt)  : '';
      case 'RESOLVED': return this.request.resolvedAt ? this.fmtDate(this.request.resolvedAt) : '';
      default: return '';
    }
  }

  advance(): void {
    if (!this.nextStatus || this.advancing()) return;
    this.advancing.set(true);
    this.dashboardService.updateStatus(this.request.id, this.nextStatus, this.comment()).subscribe({
      next: updated => {
        this.advancing.set(false);
        this.comment.set('');
        this.advanced.emit(updated);
      },
      error: () => this.advancing.set(false),
    });
  }

  ago(iso: string): string {
    const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
    if (mins < 60) return mins + 'm ago';
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
  }

  private fmtDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  ticketId(): string { return 'SH-' + this.request.id; }
}
