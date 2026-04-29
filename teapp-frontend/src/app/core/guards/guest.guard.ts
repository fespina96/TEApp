import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard para rutas públicas (login, registro).
 * Si el usuario ya tiene sesión activa, lo redirige al dashboard
 * para que no tenga que volver a iniciar sesión.
 */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  if (authService.isAuthenticated()) {
    const dest = authService.isTherapist() ? '/app/therapist' : '/app/dashboard';
    return router.createUrlTree([dest]);
  }

  return true;
};
