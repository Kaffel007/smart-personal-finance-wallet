import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) return router.createUrlTree(['/login']);
  const currentUser = auth.currentUser();
  const profile = currentUser ? of(currentUser) : auth.getCurrentUser();
  return profile.pipe(
    map(user => user.role === 'ADMIN' || router.createUrlTree(['/dashboard'])),
    catchError(() => {
      auth.logout();
      return of(router.createUrlTree(['/login']));
    }),
  );
};
