import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Protege las rutas públicas (login/registro): si ya hay sesión, redirige al área que corresponde.
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  if (authService.isAuthenticated()) {
    const dest = authService.isTherapist() ? '/app/therapist' : '/app/dashboard';
    return router.createUrlTree([dest]);
  }

  return true;
};
