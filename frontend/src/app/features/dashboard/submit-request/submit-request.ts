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
    title:       ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
    category:    ['', Validators.required],
    priority:    ['', Validators.required],
    description: ['', Validators.required],
  });

  touched = false;
  readonly loading = signal(false);
  readonly error = signal('');

  readonly CATEGORIES = [
    { value: 'IT_SUPPORT', label: 'IT Support',  dept: 'IT Support' },
    { value: 'FACILITIES', label: 'Facilities',  dept: 'Facilities' },
    { value: 'HR_REQUEST', label: 'HR Request',  dept: 'HR' },
  ];

  readonly PRIORITIES: { value: string; label: string; respondH: number; resolveH: number }[] = [
    { value: 'LOW',      label: 'Low',      respondH: 8,  resolveH: 48 },
    { value: 'MEDIUM',   label: 'Medium',   respondH: 4,  resolveH: 24 },
    { value: 'HIGH',     label: 'High',     respondH: 2,  resolveH: 8  },
    { value: 'CRITICAL', label: 'Critical', respondH: 1,  resolveH: 4  },
  ];

  get selectedCategory() {
    return this.CATEGORIES.find(c => c.value === this.form.get('category')!.value) ?? null;
  }
  get selectedPriority() {
    return this.PRIORITIES.find(p => p.value === this.form.get('priority')!.value) ?? null;
  }

  fieldInvalid(name: string): boolean {
    return this.touched && this.form.get(name)!.invalid;
  }

  get titleLength(): number { return this.form.get('title')!.value.length; }

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
