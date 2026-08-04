import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules, withRouterConfig } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { connectivityInterceptor } from './core/interceptors/connectivity.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Todas las pantallas son de carga diferida, así que la primera visita a cada
    // una tenía que esperar su chunk. Con esto se traen en segundo plano apenas
    // arranca la app, y la navegación pasa a ser inmediata.
    provideRouter(
      routes,
      withPreloading(PreloadAllModules),
      // Cuando un guard rechaza un "atrás" del navegador, la URL ya cambió.
      // Con esto el router la recalcula y vuelve a dejarla donde corresponde.
      withRouterConfig({ canceledNavigationResolution: 'computed' })
    ),
    provideHttpClient(withInterceptors([connectivityInterceptor, jwtInterceptor])),
    provideAnimations()
  ]
};
