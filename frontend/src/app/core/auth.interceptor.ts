import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isApi = request.url.startsWith(`${API_BASE_URL}/`);
  const path = request.url.split('?')[0];
  const isPublic = path === `${API_BASE_URL}/auth/login` || path === `${API_BASE_URL}/auth/register`;
  const token = isApi && !isPublic ? auth.getToken() : null;
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authenticatedRequest).pipe(catchError((error: unknown) => {
    if (isApi && !isPublic && error instanceof HttpErrorResponse && error.status === 401) {
      // Ignore a late failure from a previous session.
      if (token === auth.getToken()) auth.logout();
    }
    return throwError(() => error);
  }));
};
