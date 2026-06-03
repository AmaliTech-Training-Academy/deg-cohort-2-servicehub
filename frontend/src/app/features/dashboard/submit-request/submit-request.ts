import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DashboardService } from '../../../core/services/dashboard.service';

@Component({
  selector: 'app-submit-request',
  imports: [ReactiveFormsModule],
  templateUrl: './submit-request.html',
})
export class SubmitRequestComponent {
  private fb = inject(FormBuilder);
  private dashboardService = inject(DashboardService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    title:       ['', [Validators.required, Validators.minLength(6)]],
    category:    ['IT_SUPPORT', Validators.required],
    priority:    ['MEDIUM', Validators.required],
    description: ['', Validators.required],
  });

  touched = false;
  readonly loading = signal(false);
  readonly error = signal('');

  readonly CATEGORIES = [
    { value: 'IT_SUPPORT', label: 'IT Support',  dept: 'IT Support',  iconPath: 'M5 6h14v9H5zM3 19h18l-1-2H4l-1 2Z' },
    { value: 'FACILITIES', label: 'Facilities',  dept: 'Facilities',  iconPath: 'M14.5 6.5a3.5 3.5 0 0 0-4.6 4.3L4 16.7 7.3 20l5.9-5.9a3.5 3.5 0 0 0 4.3-4.6l-2.3 2.3-2-2 2.3-2.3Z' },
    { value: 'HR_REQUEST', label: 'HR Request',  dept: 'HR',          iconPath: 'M4 13v-1a8 8 0 0 1 16 0v1M4 13h2a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-5ZM20 13h-2a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-5ZM18 19a3 3 0 0 1-3 3h-3' },
  ];

  readonly PRIORITIES: { value: string; respondH: number; resolveH: number }[] = [
    { value: 'LOW',      respondH: 8,  resolveH: 48 },
    { value: 'MEDIUM',   respondH: 4,  resolveH: 24 },
    { value: 'HIGH',     respondH: 2,  resolveH: 8 },
    { value: 'CRITICAL', respondH: 1,  resolveH: 4 },
  ];

  get selectedCategory() { return this.CATEGORIES.find(c => c.value === this.form.get('category')!.value)!; }
  get selectedPriority() { return this.PRIORITIES.find(p => p.value === this.form.get('priority')!.value)!; }

  fieldInvalid(name: string): boolean {
    return this.touched && this.form.get(name)!.invalid;
  }

  setCategory(val: string): void { this.form.get('category')!.setValue(val); }
  setPriority(val: string): void { this.form.get('priority')!.setValue(val); }

  submit(): void {
    this.touched = true;
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set('');
    const { title, category, priority, description } = this.form.getRawValue();
    this.dashboardService.createRequest({ title, category, priority, description }).subscribe({
      next: () => { this.loading.set(false); this.router.navigateByUrl('/my-dashboard'); },
      error: err => {
        this.error.set(err?.error?.error ?? 'Failed to submit request. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
