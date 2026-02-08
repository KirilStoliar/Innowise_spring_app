import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

export function authInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Пропускаем login/register/refresh
  if (req.url.includes('/auth/login') || req.url.includes('/auth/register') || req.url.includes('/auth/refresh')) {
    return next(req);
  }

  // Добавляем токен
  const access = auth.getAccessToken();
  const withAuth = access
    ? req.clone({ setHeaders: { Authorization: `Bearer ${access}` } })
    : req;

  return next(withAuth).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401) {
        const refreshToken = auth.getRefreshToken();
        
        if (!refreshToken) {
          auth.logout();
          router.navigate(['/auth']);
          return throwError(() => err);
        }

        // Попытка обновить токен
        return auth.refresh({ refreshToken }).pipe(
          switchMap(res => {
            if (!res.success) throw err;
            
            auth.setTokens(res.data);
            
            // Повторяем запрос с новым токеном
            const retried = req.clone({
              setHeaders: { Authorization: `Bearer ${res.data.accessToken}` }
            });
            return next(retried);
          }),
          catchError(refreshErr => {
            auth.logout();
            router.navigate(['/auth']);
            return throwError(() => refreshErr);
          })
        );
      }

      return throwError(() => err);
    })
  );
}