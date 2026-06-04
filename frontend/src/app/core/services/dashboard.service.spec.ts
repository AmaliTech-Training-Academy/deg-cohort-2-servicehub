import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiUrl}/api`;

describe('DashboardService', () => {
  let service: DashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    http    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getStats() calls GET /api/dashboard/stats', () => {
    service.getStats().subscribe();
    const req = http.expectOne(`${BASE}/dashboard/stats`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalRequests: 10, openRequests: 3, resolvedRequests: 7,
      avgResolutionHours: 2.5, slaComplianceRate: 0.9,
      requestsByCategory: {}, requestsByPriority: {}, requestsByStatus: {}, slaByCategory: {} });
  });

  it('getTrends() calls GET /api/dashboard/trends with days param', () => {
    service.getTrends(7).subscribe();
    const req = http.expectOne(`${BASE}/dashboard/trends?days=7`);
    expect(req.request.method).toBe('GET');
    req.flush({ '2026-06-01': 3 });
  });

  it('getRequests() calls GET /api/requests with page and size', () => {
    service.getRequests(0, 20).subscribe();
    const req = http.expectOne(`${BASE}/requests?page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('getMyRequests() calls GET /api/requests/my-requests', () => {
    service.getMyRequests(0, 20).subscribe();
    const req = http.expectOne(`${BASE}/requests/my-requests?page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('getRequestById() calls GET /api/requests/:id', () => {
    service.getRequestById(5).subscribe();
    const req = http.expectOne(`${BASE}/requests/5`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 5, title: 'Test', status: 'OPEN' });
  });

  it('updateStatus() calls PUT /api/requests/:id/status with newStatus', () => {
    service.updateStatus(3, 'ASSIGNED').subscribe();
    const req = http.expectOne(`${BASE}/requests/3/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ newStatus: 'ASSIGNED' });
    req.flush({ id: 3, status: 'ASSIGNED' });
  });

  it('updateStatus() includes comment when provided', () => {
    service.updateStatus(3, 'ASSIGNED', 'routed to IT').subscribe();
    const req = http.expectOne(`${BASE}/requests/3/status`);
    expect(req.request.body).toEqual({ newStatus: 'ASSIGNED', comment: 'routed to IT' });
    req.flush({ id: 3, status: 'ASSIGNED' });
  });

  it('updateStatus() omits comment when blank', () => {
    service.updateStatus(3, 'ASSIGNED', '   ').subscribe();
    const req = http.expectOne(`${BASE}/requests/3/status`);
    expect(req.request.body).toEqual({ newStatus: 'ASSIGNED' });
    req.flush({ id: 3, status: 'ASSIGNED' });
  });

  it('createRequest() calls POST /api/requests', () => {
    const dto = { title: 'Broken laptop', category: 'IT_SUPPORT', priority: 'HIGH', description: 'Screen cracked' };
    service.createRequest(dto).subscribe();
    const req = http.expectOne(`${BASE}/requests`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush({ id: 1, ...dto, status: 'OPEN' });
  });
});
