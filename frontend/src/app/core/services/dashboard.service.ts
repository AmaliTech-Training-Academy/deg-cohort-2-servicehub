import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ServiceRequestResponse {
  id: number;
  title: string;
  description?: string;
  category: string;
  priority: string;
  status: string;
  departmentName?: string;
  assignedToName?: string;
  requesterName: string;
  slaDeadline?: string;
  responseDeadline?: string;
  firstResponseAt?: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  isOverdue: boolean;
  isResponseOverdue: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DashboardStatsResponse {
  totalRequests: number;
  openRequests: number;
  resolvedRequests: number;
  avgResolutionHours: number;
  slaComplianceRate: number;
  requestsByCategory: Record<string, number>;
  requestsByPriority: Record<string, number>;
}

export interface ServiceRequestDto {
  title: string;
  description: string;
  category: string;
  priority: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/api`;

  // TODO: integrate GET /api/dashboard/stats once backend PR is merged
  getStats(): Observable<DashboardStatsResponse> {
    return of({
      totalRequests: 0,
      openRequests: 0,
      resolvedRequests: 0,
      avgResolutionHours: 0,
      slaComplianceRate: 1,
      requestsByCategory: { IT_SUPPORT: 0, FACILITIES: 0, HR_REQUEST: 0 },
      requestsByPriority: { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 },
    });
  }

  getRequests(page = 0, size = 20): Observable<PageResponse<ServiceRequestResponse>> {
    return this.http.get<PageResponse<ServiceRequestResponse>>(
      `${this.BASE}/requests?page=${page}&size=${size}`
    );
  }

  getMyRequests(page = 0, size = 20): Observable<PageResponse<ServiceRequestResponse>> {
    return this.http.get<PageResponse<ServiceRequestResponse>>(
      `${this.BASE}/requests/my-requests?page=${page}&size=${size}`
    );
  }

  updateStatus(id: number, newStatus: string): Observable<ServiceRequestResponse> {
    return this.http.put<ServiceRequestResponse>(
      `${this.BASE}/requests/${id}/status`, { newStatus }
    );
  }

  createRequest(dto: ServiceRequestDto): Observable<ServiceRequestResponse> {
    return this.http.post<ServiceRequestResponse>(`${this.BASE}/requests`, dto);
  }
}
