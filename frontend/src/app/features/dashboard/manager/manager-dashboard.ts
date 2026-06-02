import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './manager-dashboard.html',
})
export class ManagerDashboard {
  private authService = inject(AuthService);
  private router = inject(Router);

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
