import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Interceptor funcional de JWT.
 * Agrega el header Authorization: Bearer <token> solo a peticiones al backend propio.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Solo añadir token a peticiones al backend propio (no a APIs externas como ARASAAC)
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  // No agregar token a las peticiones de autenticación
  if (req.url.includes('/auth/')) {
    return next(req);
  }

  const token = authService.getToken();
  if (token) {
    const authenticatedReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(authenticatedReq);
  }

  return next(req);
};
