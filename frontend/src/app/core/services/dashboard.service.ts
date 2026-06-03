import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
  slaStatus?: string;
  responseTimeMinutes?: number;
  resolutionTimeMinutes?: number;
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
  requestsByStatus: Record<string, number>;
  slaByCategory: Record<string, number>;
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

  getStats(): Observable<DashboardStatsResponse> {
    return this.http.get<DashboardStatsResponse>(`${this.BASE}/dashboard/stats`);
  }

  getTrends(days = 7): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${this.BASE}/dashboard/trends?days=${days}`
    );
  }

  getSlaCompliance(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.BASE}/dashboard/sla`);
  }

  getRequestById(id: number): Observable<ServiceRequestResponse> {
    return this.http.get<ServiceRequestResponse>(`${this.BASE}/requests/${id}`);
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
