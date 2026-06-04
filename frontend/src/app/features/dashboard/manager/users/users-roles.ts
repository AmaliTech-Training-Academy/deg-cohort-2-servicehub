import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { environment } from '../../../../../environments/environment';

interface UserRecord {
  id: number;
  fullName: string;
  email: string;
  role: string;
  department?: string;
}

@Component({
  selector: 'app-users-roles',
  templateUrl: './users-roles.html',
})
export class UsersRolesComponent implements OnInit {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly users = signal<UserRecord[]>([]);
  readonly pending = signal<UserRecord | null>(null);
  readonly promoting = signal(false);
  readonly currentEmail = this.authService.getToken()
    ? (() => { try { return JSON.parse(atob((this.authService.getToken()!.split('.')[1]).replace(/-/g,'+').replace(/_/g,'/'))).sub; } catch { return ''; } })()
    : '';

  readonly ROLE_ORDER: Record<string, number> = { MANAGER: 0, AGENT: 1, EMPLOYEE: 2 };
  readonly ROLE_CHIP: Partial<Record<string, string>> = {
    MANAGER: 'p-CRITICAL', AGENT: 'p-HIGH', EMPLOYEE: 'p-MEDIUM',
  };

  ngOnInit(): void {
    this.loadUsers();
  }

  private loadUsers(): void {
    this.http.get<UserRecord[]>(`${environment.apiUrl}/api/users`).subscribe({
      next: u => {
        this.users.set(u.sort((a, b) => (this.ROLE_ORDER[a.role] ?? 9) - (this.ROLE_ORDER[b.role] ?? 9)));
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load users.'); this.loading.set(false); },
    });
  }

  scope(u: UserRecord): string {
    if (u.role === 'MANAGER') return 'Full access · dashboards & policy';
    if (u.role === 'AGENT') return (u.department ?? 'Department') + ' queue · resolves tickets';
    return 'Submits & tracks own requests' + (u.department ? ' · ' + u.department : '');
  }

  confirmPromote(u: UserRecord): void { this.pending.set(u); }
  cancelPromote(): void { this.pending.set(null); }

  promote(): void {
    const u = this.pending();
    if (!u) return;
    this.promoting.set(true);
    this.http.put(`${environment.apiUrl}/api/users/${u.id}/role`, { role: 'AGENT' }).subscribe({
      next: () => {
        this.pending.set(null);
        this.promoting.set(false);
        this.loadUsers();
      },
      error: () => this.promoting.set(false),
    });
  }

  initials(name: string): string {
    return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  }
}
