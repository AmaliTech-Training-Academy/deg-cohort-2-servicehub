import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { LoginComponent } from './login';
import { environment } from '../../../../environments/environment';

function makeJwt(payload: Record<string, unknown>): string {
  const enc = (o: unknown) =>
    btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return `${enc({ alg: 'HS256' })}.${enc(payload)}.sig`;
}

describe('LoginComponent', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    http   = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    localStorage.clear();
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('creates the component', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('form is invalid when empty', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    expect(fixture.componentInstance.form.invalid).toBe(true);
  });

  it('form is valid with email and password', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const comp = fixture.componentInstance;
    comp.form.setValue({ email: 'a@b.com', password: 'secret' });
    expect(comp.form.valid).toBe(true);
  });

  it('does not submit when form is invalid', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.submit();
    http.expectNone(`${environment.apiUrl}/api/auth/login`);
  });

  it('sets error() signal on failed login', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const comp = fixture.componentInstance;
    comp.form.setValue({ email: 'a@b.com', password: 'wrong' });
    comp.submit();

    http.expectOne(`${environment.apiUrl}/api/auth/login`).flush(
      { error: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' }
    );

    expect(comp.error()).toBe('Invalid email or password.');
    expect(comp.loading()).toBe(false);
  });

  it('navigates to dashboard on successful login', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const comp = fixture.componentInstance;
    const nav = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const exp = Math.floor(Date.now() / 1000) + 3600;

    comp.form.setValue({ email: 'agent@a.com', password: 'pass' });
    comp.submit();

    http.expectOne(`${environment.apiUrl}/api/auth/login`).flush({
      token: makeJwt({ sub: 'agent@a.com', role: 'AGENT', exp, iat: 0 }),
      email: 'agent@a.com', role: 'AGENT', fullName: 'Agent One',
    });

    expect(nav).toHaveBeenCalledWith('/agent-dashboard');
    expect(comp.loading()).toBe(false);
  });
});
