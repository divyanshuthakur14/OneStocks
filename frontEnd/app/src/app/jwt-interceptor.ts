import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth';
import { Router } from '@angular/router';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const isAuthEndpoint = req.url.includes('/api/auth/');
  if (isAuthEndpoint) {
    return next(req);
  }

  const authReq = token
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {

      if (error.status === 401 && authService.getRefreshToken()) {
        return authService.refresh().pipe(
          switchMap((response) => {
            if (response.success) {

              const retryReq = req.clone({
                headers: req.headers.set('Authorization', `Bearer ${response.token}`)
              });
              return next(retryReq);
            }

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