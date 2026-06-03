import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-dot',
  templateUrl: './status-dot.html',
})
export class StatusDotComponent {
  @Input({ required: true }) status!: string;

  readonly STATUS_LABEL: Record<string, string> = {
    OPEN:        'Open',
    ASSIGNED:    'Assigned',
    IN_PROGRESS: 'In progress',
    RESOLVED:    'Resolved',
    CLOSED:      'Closed',
  };

  get label(): string {
    return this.STATUS_LABEL[this.status] ?? this.status;
  }
}
