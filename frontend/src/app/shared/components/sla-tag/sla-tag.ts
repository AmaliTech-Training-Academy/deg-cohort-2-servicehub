import { Component, Input, OnChanges } from '@angular/core';

interface SlaState {
  cls: 'met' | 'ok' | 'breach' | '';
  label: string;
  showAlert: boolean;
}

@Component({
  selector: 'app-sla-tag',
  templateUrl: './sla-tag.html',
})
export class SlaTagComponent implements OnChanges {
  @Input() slaDeadline?: string;
  @Input() isOverdue = false;

  state: SlaState = { cls: '', label: '—', showAlert: false };

  ngOnChanges(): void {
    this.state = this.compute();
  }

  private compute(): SlaState {
    if (this.isOverdue) {
      if (!this.slaDeadline) {
        return { cls: 'breach', label: 'Overdue', showAlert: true };
      }
      const over = this.minutesOver();
      return { cls: 'breach', label: this.formatDuration(over) + ' overdue', showAlert: true };
    }
    if (!this.slaDeadline) {
      return { cls: '', label: '—', showAlert: false };
    }
    const remaining = Math.floor((new Date(this.slaDeadline).getTime() - Date.now()) / 60000);
    if (remaining <= 0) {
      return { cls: 'breach', label: 'Overdue', showAlert: true };
    }
    if (remaining < 60) {
      return { cls: 'ok', label: remaining + 'm left', showAlert: false };
    }
    return { cls: 'met', label: this.formatDuration(remaining) + ' left', showAlert: false };
  }

  private minutesOver(): number {
    if (!this.slaDeadline) return 0;
    return Math.floor((Date.now() - new Date(this.slaDeadline).getTime()) / 60000);
  }

  private formatDuration(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  }
}
