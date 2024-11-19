import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  
  // Don't add token for OAuth2 related endpoints
  if (req.url.includes('/oauth2/authorization/google') || 
      req.url.includes('/login/oauth2/code/')) {
    return next(req);
  }

  const token = localStorage.getItem('jwtToken');

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        localStorage.clear();
        router.navigate(['/login']);
      } else if (error.status === 0 && error.error instanceof ProgressEvent) {
        console.error('Network error occurred');
      }
      return throwError(() => error);
    })
  );
};