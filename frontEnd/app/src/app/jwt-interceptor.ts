import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth';
import { Router } from '@angular/router';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // Skip adding token for auth endpoints
  const isAuthEndpoint = req.url.includes('/api/auth/');
  if (isAuthEndpoint) {
    return next(req);
  }

  // Attach token to request
  const authReq = token
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If 401 and we have a refresh token, try to refresh
      if (error.status === 401 && authService.getRefreshToken()) {
        return authService.refresh().pipe(
          switchMap((response) => {
            if (response.success) {
              // Retry original request with new token
              const retryReq = req.clone({
                headers: req.headers.set('Authorization', `Bearer ${response.token}`)
              });
              return next(retryReq);
            }
            // Refresh failed — logout and redirect
            authService.clearSession();
            router.navigate(['/login']);
            return throwError(() => error);
          }),
          catchError(() => {
            authService.clearSession();
            router.navigate(['/login']);
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};