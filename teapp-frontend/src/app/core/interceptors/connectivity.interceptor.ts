import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { ConnectivityService } from '../services/connectivity.service';
import { environment } from '../../../environments/environment';

export const connectivityInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  const connectivity = inject(ConnectivityService);

  return next(req).pipe(
    tap({
      error: (err) => {
        if (err.status === 0) {
          connectivity.markDisconnected();
        }
      }
    })
  );
};
