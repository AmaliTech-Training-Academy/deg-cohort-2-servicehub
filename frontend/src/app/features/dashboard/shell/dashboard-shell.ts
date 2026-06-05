import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem { icon: string; label: string; route: string; }

const NAV: Record<string, NavItem[]> = {
  EMPLOYEE: [
    { icon: 'home',  label: 'My Requests',     route: '/my-dashboard' },
    { icon: 'plus',  label: 'Submit Request',   route: '/requests/submit' },
  ],
  AGENT: [
    { icon: 'inbox',  label: 'Department Queue', route: '/agent-dashboard' },
  ],
  MANAGER: [
    { icon: 'chart', label: 'Dashboard',    route: '/dashboard' },
    { icon: 'grid',  label: 'All Tickets',  route: '/all-tickets' },
    { icon: 'clock', label: 'SLA Policies', route: '/sla-policies' },
  ],
};

const NAV_GROUP: Record<string, string> = {
  EMPLOYEE: 'My Workspace',
  AGENT:    'Support Desk',
  MANAGER:  'Administration',
};

const ICONS: Record<string, string> = {
  home:   'M3 11.5 12 4l9 7.5M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9',
  plus:   'M12 5v14M5 12h14',
  inbox:  'M3 13h4l2 3h6l2-3h4M5 5h14l2 8v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5L5 5Z',
  chart:  'M4 20V10M10 20V4M16 20v-7M22 20H2',
  grid:   'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  clock:  'M12 7v5l3 2M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Z',
  users:  'M16 19v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1M9 10a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM22 19v-1a4 4 0 0 0-3-3.87M16 4.13A4 4 0 0 1 16 11.87',
  menu:   'M4 6h16M4 12h16M4 18h16',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  plus2:  'M12 5v14M5 12h14',
  chevron:'M9 18l6-6-6-6',
};

const ROLE_LABEL: Record<string, string> = {
  EMPLOYEE: 'Employee', AGENT: 'Agent', MANAGER: 'Manager',
};

@Component({
  selector: 'app-dashboard-shell',
  imports: [RouterOutlet],
  templateUrl: './dashboard-shell.html',
})
export class DashboardShell implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);

  navOpen = signal(false);           // mobile overlay
  sidebarCollapsed = signal(false);  // desktop collapse (false = expanded)
  pageTitle = signal('');
  pageSubtitle = signal('');

  get role(): string { return this.authService.getUserRole() ?? ''; }
  get navItems(): NavItem[] { return NAV[this.role] ?? []; }
  get navGroup(): string { return NAV_GROUP[this.role] ?? ''; }
  get roleLabel(): string { return ROLE_LABEL[this.role] ?? this.role; }
  get fullName(): string { return this.authService.getFullName(); }
  get initials(): string {
    return this.fullName.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  }
  get isEmployee(): boolean { return this.role === 'EMPLOYEE'; }
  get showNewRequestCta(): boolean {
    return this.isEmployee && !this.router.url.startsWith('/requests/submit');
  }

  iconPath(name: string): string { return ICONS[name] ?? ''; }
  isActive(route: string): boolean { return this.router.url === route || this.router.url.startsWith(route + '?'); }

  ngOnInit(): void {
    this.updateTitle();
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => this.updateTitle());
  }

  private updateTitle(): void {
    let child = this.activatedRoute.firstChild;
    while (child?.firstChild) child = child.firstChild;
    const data = child?.snapshot.data ?? {};
    this.pageTitle.set(data['title'] ?? 'ServiceHub');
    this.pageSubtitle.set(data['subtitle'] ?? '');
  }

  navigate(route: string): void { this.navOpen.set(false); this.router.navigateByUrl(route); }
  logout(): void { this.authService.logout(); this.router.navigateByUrl('/login'); }
}
