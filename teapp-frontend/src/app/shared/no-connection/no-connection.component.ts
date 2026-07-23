import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { filter, take } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConnectivityService } from '../../core/services/connectivity.service';

@Component({
  selector: 'app-no-connection',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <div class="overlay" *ngIf="(connectivity.connected$ | async) === false">
      <div class="overlay-card">
        <span class="overlay-icon">📡</span>
        <h2>Sin conexión al servidor</h2>
        <p>No se pudo conectar con el servidor. Verificá tu conexión a internet y que el servidor esté activo.</p>
        <button mat-flat-button color="primary" (click)="retry()" [disabled]="retrying">
          <mat-spinner diameter="18" *ngIf="retrying"></mat-spinner>
          <span *ngIf="!retrying">Reintentar conexión</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(240, 240, 240, 0.97);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .overlay-card {
      background: #fff;
      border-radius: 20px;
      padding: 48px 36px;
      max-width: 420px;
      width: 90%;
      text-align: center;
      box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    }
    .overlay-icon { font-size: 3.5rem; display: block; margin-bottom: 16px; }
    h2 {
      font-size: 1.4rem;
      font-weight: 700;
      color: #2C3E50;
      margin: 0 0 8px;
    }
    p {
      color: #6C757D;
      font-size: 0.95rem;
      line-height: 1.5;
      margin: 0 0 24px;
    }
    button {
      min-width: 200px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin: 0 auto;
    }
  `]
})
export class NoConnectionComponent {
  retrying = false;

  constructor(public connectivity: ConnectivityService) {}

  retry(): void {
    this.retrying = true;
    this.connectivity.check();
    this.connectivity.connected$.pipe(
      filter(v => v),
      take(1)
    ).subscribe(() => { this.retrying = false; });
  }
}
