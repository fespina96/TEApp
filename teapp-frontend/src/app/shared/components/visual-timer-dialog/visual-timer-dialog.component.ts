import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface VisualTimerData {
  activityName: string;
  activityColor: string;
  pictogramUrl?: string;
  imageBase64?: string;
  durationMinutes: number;
  pausable: boolean;
  requireFullTimer: boolean;
}

const CIRCUNFERENCIA = 2 * Math.PI * 90; // radio 90

@Component({
  selector: 'app-visual-timer-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div class="timer-wrapper">

      <!-- Imagen / pictograma -->
      <img *ngIf="data.pictogramUrl || data.imageBase64"
           [src]="data.pictogramUrl || data.imageBase64"
           class="timer-pictogram" alt="">

      <h2 class="timer-activity-name">{{ data.activityName }}</h2>

      <!-- Círculo SVG -->
      <div class="timer-circle-wrap">
        <svg viewBox="0 0 200 200" class="timer-svg">
          <!-- Pista -->
          <circle cx="100" cy="100" r="90" class="track"/>
          <!-- Progreso -->
          <circle cx="100" cy="100" r="90" class="progress"
                  [style.stroke]="data.activityColor"
                  [style.stroke-dasharray]="circunferencia"
                  [style.stroke-dashoffset]="desfase"/>
        </svg>
        <div class="timer-center" [class.timer-done]="terminado">
          <ng-container *ngIf="!terminado">
            <span class="timer-time">{{ tiempoTexto }}</span>
            <span class="timer-label">{{ pausado ? 'pausado' : 'restante' }}</span>
          </ng-container>
          <ng-container *ngIf="terminado">
            <mat-icon class="done-icon">check_circle</mat-icon>
            <span class="done-text">¡Listo!</span>
          </ng-container>
        </div>
      </div>

      <!-- Controles -->
      <div class="timer-controls">
        <button mat-flat-button class="btn-pause"
                *ngIf="data.pausable && !terminado"
                (click)="alternarPausa()">
          <mat-icon>{{ pausado ? 'play_arrow' : 'pause' }}</mat-icon>
          {{ pausado ? 'Continuar' : 'Pausar' }}
        </button>

        <button mat-flat-button class="btn-done"
                *ngIf="!terminado && !data.requireFullTimer"
                (click)="marcarTerminado()">
          <mat-icon>check_circle</mat-icon>
          Terminado
        </button>

        <button mat-stroked-button class="btn-close"
                *ngIf="terminado"
                (click)="cerrar()">
          <mat-icon>check</mat-icon>
          Cerrar
        </button>

        <button mat-stroked-button class="btn-close"
                *ngIf="!terminado && !data.requireFullTimer"
                (click)="cancelar()">
          <mat-icon>close</mat-icon>
          Cancelar
        </button>
      </div>

    </div>
  `,
  styles: [`
    .timer-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 24px 24px 16px;
      min-width: 280px;
    }

    .timer-pictogram {
      width: 80px;
      height: 80px;
      object-fit: contain;
      border-radius: 12px;
      background: rgba(0,0,0,0.04);
      padding: 4px;
    }

    .timer-activity-name {
      margin: 0;
      font-size: 1.3rem;
      font-weight: 700;
      text-align: center;
      color: #2C3E50;
    }

    .timer-circle-wrap {
      position: relative;
      width: 200px;
      height: 200px;
    }

    .timer-svg {
      width: 200px;
      height: 200px;
      transform: rotate(-90deg);

      .track {
        fill: none;
        stroke: #E9ECEF;
        stroke-width: 14;
      }

      .progress {
        fill: none;
        stroke-width: 14;
        stroke-linecap: round;
        transition: stroke-dashoffset 0.9s linear;
      }
    }

    .timer-center {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;

      .timer-time {
        font-size: 2.4rem;
        font-weight: 800;
        color: #2C3E50;
        line-height: 1;
      }

      .timer-label {
        font-size: 0.78rem;
        color: #6C757D;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      &.timer-done {
        .done-icon {
          font-size: 3.5rem;
          width: 3.5rem;
          height: 3.5rem;
          color: #27AE60;
        }
        .done-text {
          font-size: 1.5rem;
          font-weight: 800;
          color: #27AE60;
        }
      }
    }

    .timer-controls {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      justify-content: center;

      .btn-pause {
        background: #A8D8EA !important;
        color: #2C3E50 !important;
        border-radius: 24px !important;
        font-weight: 600;
        min-width: 130px;
      }

      .btn-done {
        background: #27AE60 !important;
        color: #fff !important;
        border-radius: 24px !important;
        font-weight: 600;
        min-width: 130px;
      }

      .btn-close {
        border-radius: 24px !important;
        min-width: 110px;
      }
    }
  `]
})
export class VisualTimerDialogComponent implements OnInit, OnDestroy {

  readonly circunferencia = CIRCUNFERENCIA;

  segundosTotales = 0;
  segundosRestantes = 0;
  pausado = false;
  terminado = false;

  private idIntervalo?: ReturnType<typeof setInterval>;

  constructor(
    public dialogRef: MatDialogRef<VisualTimerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VisualTimerData
  ) {}

  ngOnInit(): void {
    this.segundosTotales = this.data.durationMinutes * 60;
    this.segundosRestantes = this.segundosTotales;
    this.iniciarConteo();
  }

  ngOnDestroy(): void {
    clearInterval(this.idIntervalo);
  }

  get desfase(): number {
    const progreso = this.segundosRestantes / this.segundosTotales;
    return CIRCUNFERENCIA * (1 - progreso);
  }

  get tiempoTexto(): string {
    const m = Math.floor(this.segundosRestantes / 60);
    const s = this.segundosRestantes % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  alternarPausa(): void {
    this.pausado = !this.pausado;
    if (!this.pausado) this.iniciarConteo();
    else clearInterval(this.idIntervalo);
  }

  marcarTerminado(): void {
    clearInterval(this.idIntervalo);
    this.terminado = true;
  }

  cerrar(): void {
    this.dialogRef.close(true);
  }

  cancelar(): void {
    this.dialogRef.close(false);
  }

  private iniciarConteo(): void {
    clearInterval(this.idIntervalo);
    this.idIntervalo = setInterval(() => {
      if (this.segundosRestantes > 0) {
        this.segundosRestantes--;
      } else {
        clearInterval(this.idIntervalo);
        this.terminado = true;
      }
    }, 1000);
  }
}
