import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { DashboardShell } from './features/dashboard/shell/dashboard-shell';
import { ManagerDashboard } from './features/dashboard/manager/manager-dashboard';
import { AllTicketsComponent } from './features/dashboard/manager/all-tickets/all-tickets';
import { SlaPoliciesComponent } from './features/dashboard/manager/sla-policies/sla-policies';
import { UsersRolesComponent } from './features/dashboard/manager/users/users-roles';
import { AgentDashboard } from './features/dashboard/agent/agent-dashboard';
import { EmployeeDashboard } from './features/dashboard/employee/employee-dashboard';
import { SubmitRequestComponent } from './features/dashboard/submit-request/submit-request';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: DashboardShell,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        component: ManagerDashboard,
        canActivate: [roleGuard('MANAGER')],
        data: { title: 'Dashboard', subtitle: 'Operations overview' },
      },
      {
        path: 'all-tickets',
        component: AllTicketsComponent,
        canActivate: [roleGuard('MANAGER')],
        data: { title: 'All Tickets', subtitle: 'Every department' },
      },
      {
        path: 'sla-policies',
        component: SlaPoliciesComponent,
        canActivate: [roleGuard('MANAGER')],
        data: { title: 'SLA Policies', subtitle: 'Response & resolution targets' },
      },
      {
        path: 'users',
        component: UsersRolesComponent,
        canActivate: [roleGuard('MANAGER')],
        data: { title: 'Users & Roles', subtitle: 'Access control' },
      },
      {
        path: 'agent-dashboard',
        component: AgentDashboard,
        canActivate: [roleGuard('AGENT')],
        data: { title: 'Department Queue', subtitle: 'Auto-routed tickets' },
      },
      {
        path: 'my-dashboard',
        component: EmployeeDashboard,
        canActivate: [roleGuard('EMPLOYEE')],
        data: { title: 'My Requests', subtitle: 'Your submitted tickets' },
      },
      {
        path: 'requests/submit',
        component: SubmitRequestComponent,
        canActivate: [roleGuard('EMPLOYEE')],
        data: { title: 'Submit Request', subtitle: 'New service request' },
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
