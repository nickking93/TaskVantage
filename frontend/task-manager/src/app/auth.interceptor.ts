import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap } from 'rxjs/operators';
import { from } from 'rxjs';
import { throwError, Observable, iif, of } from 'rxjs';
import { AuthService } from './services/auth.service';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // Skip token checks for OAuth2 related endpoints
  if (req.url.includes('/oauth2/authorization/google') || 
      req.url.includes('/login/oauth2/code/') ||
      req.url.includes('/api/login') ||
      req.url.includes('/api/register') ||
      req.url.includes('/api/verify-email') ||
      req.url.includes('/api/forgot-password') ||
      req.url.includes('/api/reset-password')) {
    return next(req);
  }

  // Wrap the token retrieval and request handling in a from() to handle the Promise
  return from(authService.getAuthToken()).pipe(
    switchMap(token => {
      if (!token) {
        // No token available, proceed without authentication
        return next(req);
      }

      // Clone the request with the token
      const authReq = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });

      // Process the authenticated request
      return next(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
          if (error.status === 401) {
            // Token might be expired, try to refresh
            if (!isRefreshing) {
              isRefreshing = true;
              
              return authService.refreshToken().pipe(
                switchMap(newToken => {
                  isRefreshing = false;
                  // Clone request with new token
                  const newAuthReq = req.clone({
                    headers: req.headers.set('Authorization', `Bearer ${newToken}`)
                  });
                  return next(newAuthReq);
                }),
                catchError(refreshError => {
                  isRefreshing = false;
                  // If refresh fails, clear auth state and redirect to login
                  authService.logout().subscribe(() => {
                    router.navigate(['/login']);
                  });
                  return throwError(() => refreshError);
                })
              );
            } else {
              // If refresh is already in progress, wait and retry original request
              return of(error).pipe(
                switchMap(() => {
                  return from(authService.getAuthToken()).pipe(
                    switchMap(newToken => {
                      const retryReq = req.clone({
                        headers: req.headers.set('Authorization', `Bearer ${newToken}`)
                      });
                      return next(retryReq);
                    })
                  );
                })
              );
            }
          } else if (error.status === 0 && error.error instanceof ProgressEvent) {
            // Network error occurred
            console.error('Network error occurred');
            return throwError(() => new Error('Network error occurred'));
          }
          
          // For other errors, pass through
          return throwError(() => error);
        })
      );
    })
  );
};