import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ActivityStep } from '../../../core/models/activity.model';

export interface StepViewerData {
  activityName: string;
  activityColor: string;
  steps: ActivityStep[];
}

@Component({
  selector: 'app-step-viewer-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div class="viewer-wrapper">

      <!-- Cabecera -->
      <div class="viewer-header" [style.background-color]="data.activityColor + '66'">
        <span class="viewer-activity">{{ data.activityName }}</span>
        <span class="viewer-counter">{{ current + 1 }} / {{ data.steps.length }}</span>
      </div>

      <!-- Contenido del paso -->
      <div class="viewer-step">
        <div class="step-image-wrap"
             *ngIf="step.pictogramUrl || step.imageBase64">
          <img [src]="step.pictogramUrl || step.imageBase64" class="step-image" alt="">
        </div>

        <div class="step-number" [style.background-color]="data.activityColor">
          {{ current + 1 }}
        </div>

        <h2 class="step-title">{{ step.title }}</h2>

        <p class="step-desc" *ngIf="step.description">{{ step.description }}</p>
      </div>

      <!-- Barra de progreso -->
      <div class="progress-bar">
        <div class="progress-fill"
             [style.width.%]="((current + 1) / data.steps.length) * 100"
             [style.background-color]="data.activityColor">
        </div>
      </div>

      <!-- Controles -->
      <div class="viewer-controls">
        <button mat-stroked-button (click)="prev()" [disabled]="current === 0">
          <mat-icon>arrow_back</mat-icon>
          Anterior
        </button>

        <button mat-flat-button class="btn-next"
                *ngIf="!isLast"
                [style.background-color]="data.activityColor"
                (click)="next()">
          Siguiente
          <mat-icon>arrow_forward</mat-icon>
        </button>

        <button mat-flat-button class="btn-finish"
                *ngIf="isLast"
                (click)="finish()">
          <mat-icon>check_circle</mat-icon>
          ¡Listo!
        </button>
      </div>

    </div>
  `,
  styles: [`
    .viewer-wrapper {
      display: flex; flex-direction: column;
      min-width: 300px; max-width: 420px;
    }

    .viewer-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 20px; border-radius: 4px 4px 0 0;
      .viewer-activity { font-weight: 700; font-size: 1rem; color: #2C3E50; }
      .viewer-counter { font-size: 0.85rem; color: #6C757D; font-weight: 600; }
    }

    .viewer-step {
      display: flex; flex-direction: column; align-items: center;
      gap: 12px; padding: 24px 24px 16px;

      .step-image-wrap {
        width: 180px; height: 180px;
        img.step-image { width: 100%; height: 100%; object-fit: contain;
                         border-radius: 16px; background: #f8f9fa; padding: 8px; }
      }

      .step-number {
        width: 36px; height: 36px; border-radius: 50%;
        display: flex; align-items: center; justify-content: center;
        font-weight: 800; font-size: 1.1rem; color: #2C3E50;
      }

      .step-title {
        margin: 0; font-size: 1.4rem; font-weight: 700;
        text-align: center; color: #2C3E50;
      }

      .step-desc {
        margin: 0; font-size: 0.95rem; color: #6C757D;
        text-align: center; line-height: 1.5;
      }
    }

    .progress-bar {
      height: 6px; background: #E9ECEF; margin: 0 16px;
      border-radius: 3px; overflow: hidden;
      .progress-fill { height: 100%; border-radius: 3px; transition: width 0.3s ease; }
    }

    .viewer-controls {
      display: flex; justify-content: space-between; align-items: center;
      padding: 16px 20px; gap: 12px;

      .btn-next { color: #2C3E50 !important; font-weight: 600;
                  border-radius: 24px !important; }
      .btn-finish { background: #27AE60 !important; color: #fff !important;
                    font-weight: 700; border-radius: 24px !important; }
      button[mat-stroked-button] { border-radius: 24px !important; }
    }
  `]
})
export class StepViewerDialogComponent {
  current = 0;

  constructor(
    public dialogRef: MatDialogRef<StepViewerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: StepViewerData
  ) {}

  get step(): ActivityStep { return this.data.steps[this.current]; }
  get isLast(): boolean { return this.current === this.data.steps.length - 1; }

  prev(): void { if (this.current > 0) this.current--; }
  next(): void { if (!this.isLast) this.current++; }
  finish(): void { this.dialogRef.close(true); }
}
