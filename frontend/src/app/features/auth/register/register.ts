import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    name:       ['', [Validators.required, Validators.minLength(2)]],
    email:      ['', [Validators.required, Validators.email]],
    password:   ['', [Validators.required, Validators.minLength(6)]],
    department: [''],
  });

  error = '';
  loading = false;

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const { name, email, password, department } = this.form.getRawValue();
    this.authService.register({ name, email, password, department: department || undefined }).subscribe({
      next: () => { this.loading = false; this.router.navigateByUrl(this.authService.getDashboardRoute()); },
      error: (err) => {
        this.error = err?.error?.error ?? 'Registration failed. Please try again.';
        this.loading = false;
      },
    });
  }
}
