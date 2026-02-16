import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminCanMatch: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/auth']);
  }
  
  if (!auth.isAdmin()) {
    return router.createUrlTree(['/orders']);
  }
  
  return true;
};