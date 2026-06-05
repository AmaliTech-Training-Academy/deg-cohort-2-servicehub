import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

interface SlaPolicy { id: number; category: string; priority: string; responseTimeHours: number; resolutionTimeHours: number; }

@Component({
  selector: 'app-sla-policies',
  templateUrl: './sla-policies.html',
})
export class SlaPoliciesComponent implements OnInit {
  private http = inject(HttpClient);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly policies = signal<SlaPolicy[]>([]);

  readonly PRIORITY_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  readonly CAT_LABEL: Partial<Record<string, string>> = {
    IT_SUPPORT: 'IT Support', FACILITIES: 'Facilities', HR_REQUEST: 'HR Request',
  };
  readonly PRIO_USE: Record<string, string> = {
    CRITICAL: 'System outage, data loss, immediate business impact',
    HIGH:     'Core business function disrupted',
    MEDIUM:   'Significant inconvenience, workaround exists',
    LOW:      'Minor issue, no business impact',
  };
  readonly DEPT_ROUTING = [
    { cat: 'IT_SUPPORT', dept: 'IT Support team', email: 'it@amalitech.com' },
    { cat: 'FACILITIES', dept: 'Facilities team', email: 'facilities@amalitech.com' },
    { cat: 'HR_REQUEST', dept: 'HR team',         email: 'hr@amalitech.com' },
  ];

  ngOnInit(): void {
    this.http.get<SlaPolicy[]>(`${environment.apiUrl}/api/sla/policies`).subscribe({
      next: p => { this.policies.set(p); this.loading.set(false); },
      error: () => { this.error.set('Failed to load SLA policies.'); this.loading.set(false); },
    });
  }

  sortedPolicies(): SlaPolicy[] {
    return [...this.policies()].sort((a, b) =>
      this.PRIORITY_ORDER.indexOf(a.priority) - this.PRIORITY_ORDER.indexOf(b.priority)
    );
  }
}
