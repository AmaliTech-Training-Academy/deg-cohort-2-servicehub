import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) return true;

  return router.createUrlTree(['/login']);
};

export const roleGuard = (allowedRole: UserRole): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getUserRole() === allowedRole) return true;

  return router.createUrlTree([authService.getDashboardRoute()]);
};
