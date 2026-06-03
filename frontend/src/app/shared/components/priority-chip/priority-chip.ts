import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-priority-chip',
  templateUrl: './priority-chip.html',
})
export class PriorityChipComponent {
  @Input({ required: true }) priority!: string;
}
