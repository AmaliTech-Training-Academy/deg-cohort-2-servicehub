import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export type UserRole = 'MANAGER' | 'AGENT' | 'EMPLOYEE';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  department?: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: UserRole;
  fullName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly NAME_KEY = 'auth_name';
  private readonly API = `${environment.apiUrl}/api/auth`;
  private http = inject(HttpClient);

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API}/login`, credentials)
      .pipe(tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.NAME_KEY, res.fullName);
      }));
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API}/register`, data)
      .pipe(tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.NAME_KEY, res.fullName);
      }));
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.NAME_KEY);
  }

  getFullName(): string {
    return localStorage.getItem(this.NAME_KEY) ?? 'User';
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const { exp } = this.decodeToken(token);
      return exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  getUserRole(): UserRole | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      return this.decodeToken(token).role as UserRole;
    } catch {
      return null;
    }
  }

  getDashboardRoute(): string {
    switch (this.getUserRole()) {
      case 'MANAGER':  return '/dashboard';
      case 'AGENT':    return '/agent-dashboard';
      case 'EMPLOYEE': return '/my-dashboard';
      default:         return '/login';
    }
  }

  private decodeToken(token: string): { exp: number; role: string } {
    const payload = token.split('.')[1] ?? '';
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
    return JSON.parse(atob(padded));
  }
}
