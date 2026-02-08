import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

function redirectToAuth(returnUrl: string) {
  const router = inject(Router);
  return router.createUrlTree(['/auth'], { queryParams: { returnUrl } });
}

export const authCanActivate: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? true : redirectToAuth(state.url);
};

export const authCanMatch: CanMatchFn = (route, segments) => {
  const auth = inject(AuthService);
  const returnUrl = '/' + segments.map(s => s.path).join('/');
  return auth.isAuthenticated() ? true : redirectToAuth(returnUrl);
};