import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

/* Helper — creates a base64url-encoded JWT with the given payload */
function makeJwt(payload: Record<string, unknown>): string {
  const enc = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return `${enc({ alg: 'HS256' })}.${enc(payload)}.sig`;
}

const FUTURE_EXP = Math.floor(Date.now() / 1000) + 3600;
const PAST_EXP   = Math.floor(Date.now() / 1000) - 1;

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http    = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  /* ── login ── */
  describe('login()', () => {
    it('POSTs to /api/auth/login and stores token and fullName', () => {
      const token = makeJwt({ sub: 'a@b.com', role: 'EMPLOYEE', exp: FUTURE_EXP, iat: 0 });
      let result: unknown;
      service.login({ email: 'a@b.com', password: 'pass' }).subscribe(r => (result = r));

      const req = http.expectOne(`${environment.apiUrl}/api/auth/login`);
      expect(req.request.method).toBe('POST');
      req.flush({ token, email: 'a@b.com', role: 'EMPLOYEE', fullName: 'Alice' });

      expect(localStorage.getItem('auth_token')).toBe(token);
      expect(localStorage.getItem('auth_name')).toBe('Alice');
      expect(result).toEqual({ token, email: 'a@b.com', role: 'EMPLOYEE', fullName: 'Alice' });
    });
  });

  /* ── register ── */
  describe('register()', () => {
    it('POSTs to /api/auth/register and stores token and fullName', () => {
      const token = makeJwt({ sub: 'b@b.com', role: 'EMPLOYEE', exp: FUTURE_EXP, iat: 0 });
      service.register({ name: 'Bob', email: 'b@b.com', password: '123456', department: 'IT Support' }).subscribe();

      const req = http.expectOne(`${environment.apiUrl}/api/auth/register`);
      expect(req.request.method).toBe('POST');
      req.flush({ token, email: 'b@b.com', role: 'EMPLOYEE', fullName: 'Bob' });

      expect(localStorage.getItem('auth_token')).toBe(token);
      expect(localStorage.getItem('auth_name')).toBe('Bob');
    });
  });

  /* ── logout ── */
  describe('logout()', () => {
    it('removes token and name from localStorage', () => {
      localStorage.setItem('auth_token', 'tok');
      localStorage.setItem('auth_name', 'Alice');
      service.logout();
      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('auth_name')).toBeNull();
    });
  });

  /* ── getToken ── */
  describe('getToken()', () => {
    it('returns null when not logged in', () => expect(service.getToken()).toBeNull());
    it('returns the stored token', () => {
      localStorage.setItem('auth_token', 'my-token');
      expect(service.getToken()).toBe('my-token');
    });
  });

  /* ── getFullName ── */
  describe('getFullName()', () => {
    it('returns "User" when no name stored', () => expect(service.getFullName()).toBe('User'));
    it('returns the stored name', () => {
      localStorage.setItem('auth_name', 'Carol');
      expect(service.getFullName()).toBe('Carol');
    });
  });

  /* ── isLoggedIn ── */
  describe('isLoggedIn()', () => {
    it('returns false when no token', () => expect(service.isLoggedIn()).toBe(false));
    it('returns true for a valid non-expired token', () => {
      localStorage.setItem('auth_token', makeJwt({ exp: FUTURE_EXP, role: 'EMPLOYEE' }));
      expect(service.isLoggedIn()).toBe(true);
    });
    it('returns false for an expired token', () => {
      localStorage.setItem('auth_token', makeJwt({ exp: PAST_EXP, role: 'EMPLOYEE' }));
      expect(service.isLoggedIn()).toBe(false);
    });
  });

  /* ── getUserRole ── */
  describe('getUserRole()', () => {
    it('returns null when no token', () => expect(service.getUserRole()).toBeNull());
    it.each(['MANAGER', 'AGENT', 'EMPLOYEE'] as const)('returns %s from JWT', (role) => {
      localStorage.setItem('auth_token', makeJwt({ exp: FUTURE_EXP, role }));
      expect(service.getUserRole()).toBe(role);
    });
  });

  /* ── getDashboardRoute ── */
  describe('getDashboardRoute()', () => {
    it.each([
      ['MANAGER',  '/dashboard'],
      ['AGENT',    '/agent-dashboard'],
      ['EMPLOYEE', '/my-dashboard'],
    ] as Array<[string, string]>)('routes %s to %s', (role, route) => {
      localStorage.setItem('auth_token', makeJwt({ exp: FUTURE_EXP, role }));
      expect(service.getDashboardRoute()).toBe(route);
    });
    it('falls back to /login when no token', () => {
      expect(service.getDashboardRoute()).toBe('/login');
    });
  });
});
