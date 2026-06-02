import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login';
import { ManagerDashboard } from './features/dashboard/manager/manager-dashboard';
import { AgentDashboard } from './features/dashboard/agent/agent-dashboard';
import { EmployeeDashboard } from './features/dashboard/employee/employee-dashboard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: ManagerDashboard,
    canActivate: [authGuard, roleGuard('MANAGER')],
  },
  {
    path: 'agent-dashboard',
    component: AgentDashboard,
    canActivate: [authGuard, roleGuard('AGENT')],
  },
  {
    path: 'my-dashboard',
    component: EmployeeDashboard,
    canActivate: [authGuard, roleGuard('EMPLOYEE')],
  },
  { path: '**', redirectTo: 'login' },
];
