import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

interface Department {
  id: number;
  name: string;
  category: string;
  isActive: boolean;
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private http = inject(HttpClient);

  readonly departments = signal<Department[]>([]);
  readonly deptLoading = signal(true);

  form = this.fb.nonNullable.group({
    name:       ['', [Validators.required, Validators.minLength(2)]],
    email:      ['', [Validators.required, Validators.email]],
    password:   ['', [Validators.required, Validators.minLength(6)]],
    department: ['', Validators.required],
  });

  error = '';
  loading = false;

  ngOnInit(): void {
    this.http.get<Department[]>(`${environment.apiUrl}/api/departments`).subscribe({
      next: depts => { this.departments.set(depts); this.deptLoading.set(false); },
      error: () => this.deptLoading.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const { name, email, password, department } = this.form.getRawValue();
    this.authService.register({ name, email, password, department }).subscribe({
      next: () => { this.loading = false; this.router.navigateByUrl(this.authService.getDashboardRoute()); },
      error: (err) => {
        this.error = err?.error?.error ?? 'Registration failed. Please try again.';
        this.loading = false;
      },
    });
  }
}
