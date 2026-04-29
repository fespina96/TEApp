import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

/**
 * Definición de rutas principal de la aplicación.
 * Las rutas protegidas requieren el guard de autenticación JWT.
 */
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/components/header/header.component').then(m => m.HeaderComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'children',
        loadComponent: () =>
          import('./features/children/child-list/child-list.component').then(m => m.ChildListComponent)
      },
      {
        path: 'children/new',
        loadComponent: () =>
          import('./features/children/child-form/child-form.component').then(m => m.ChildFormComponent)
      },
      {
        path: 'children/:childId/edit',
        loadComponent: () =>
          import('./features/children/child-form/child-form.component').then(m => m.ChildFormComponent)
      },
      {
        path: 'children/:childId/agenda',
        loadComponent: () =>
          import('./features/agenda/agenda-view/agenda-view.component').then(m => m.AgendaViewComponent)
      },
      {
        path: 'activities',
        loadComponent: () =>
          import('./features/activities/activity-catalog/activity-catalog.component').then(m => m.ActivityCatalogComponent)
      },
      {
        path: 'therapist',
        loadComponent: () =>
          import('./features/therapist/therapist-dashboard/therapist-dashboard.component')
            .then(m => m.TherapistDashboardComponent)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component')
            .then(m => m.ProfileComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
