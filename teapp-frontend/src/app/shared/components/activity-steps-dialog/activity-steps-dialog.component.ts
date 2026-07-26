import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivityService } from '../../../core/services/activity.service';
import { ArasaacService } from '../../../core/services/arasaac.service';
import { Activity, ActivityStep, ActivityStepRequest } from '../../../core/models/activity.model';

export interface ActivityStepsDialogData { activity: Activity; }

interface StepDraft {
  title: string;
  description: string;
  imageBase64: string | null;
  pictogramUrl: string | null;
  busquedaArasaac: string;
  resultadosArasaac: { _id: number; keyword: string }[];
  buscandoArasaac: boolean;
  modoImagen: 'upload' | 'arasaac';
}

@Component({
  selector: 'app-activity-steps-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>list</mat-icon>
      Pasos de "{{ data.activity.name }}"
    </h2>

    <mat-dialog-content class="steps-content">
      <div class="steps-list" *ngIf="!cargando">

        <div class="step-card" *ngFor="let step of steps; let i = index">
          <div class="step-num">{{ i + 1 }}</div>

          <div class="step-body">
            <!-- Título -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Título del paso</mat-label>
              <input matInput [(ngModel)]="step.title" placeholder="Ej: Abrir el grifo">
            </mat-form-field>

            <!-- Imagen -->
            <div class="image-mode-tabs">
              <button type="button" class="img-mode-btn" [class.active]="step.modoImagen === 'upload'"
                      (click)="step.modoImagen = 'upload'">
                <mat-icon>upload</mat-icon> Subir
              </button>
              <button type="button" class="img-mode-btn" [class.active]="step.modoImagen === 'arasaac'"
                      (click)="step.modoImagen = 'arasaac'">
                <mat-icon>image_search</mat-icon> ARASAAC
              </button>
            </div>

            <!-- Upload -->
            <div *ngIf="step.modoImagen === 'upload'">
              <ng-container *ngIf="!step.imageBase64">
                <label class="upload-placeholder" [for]="'stepImg' + i">
                  <mat-icon>add_photo_alternate</mat-icon>
                  <input [id]="'stepImg' + i" type="file" accept="image/*"
                         (change)="alSeleccionarImagen($event, step)" hidden>
                </label>
              </ng-container>
              <ng-container *ngIf="step.imageBase64">
                <div class="img-preview-wrap">
                  <img [src]="step.imageBase64" class="step-img-preview" alt="">
                  <button mat-icon-button (click)="step.imageBase64 = null" matTooltip="Quitar">
                    <mat-icon>close</mat-icon>
                  </button>
                </div>
              </ng-container>
            </div>

            <!-- ARASAAC -->
            <div *ngIf="step.modoImagen === 'arasaac'" class="arasaac-mini">
              <div class="arasaac-search-row">
                <input class="arasaac-input" [(ngModel)]="step.busquedaArasaac"
                       placeholder="Buscar pictograma..." (keyup.enter)="buscarArasaac(step)">
                <button mat-icon-button (click)="buscarArasaac(step)" [disabled]="step.buscandoArasaac">
                  <mat-spinner *ngIf="step.buscandoArasaac" diameter="16"></mat-spinner>
                  <mat-icon *ngIf="!step.buscandoArasaac">search</mat-icon>
                </button>
              </div>
              <div class="arasaac-grid-mini" *ngIf="step.resultadosArasaac.length > 0">
                <button type="button" class="arasaac-thumb"
                        *ngFor="let pic of step.resultadosArasaac"
                        [class.selected]="step.pictogramUrl === arasaacService.imageUrl(pic._id)"
                        (click)="step.pictogramUrl = arasaacService.imageUrl(pic._id)"
                        [matTooltip]="pic.keyword">
                  <img [src]="arasaacService.thumbUrl(pic._id)" [alt]="pic.keyword" loading="lazy">
                </button>
              </div>
              <div class="selected-pictogram" *ngIf="step.pictogramUrl">
                <img [src]="step.pictogramUrl" alt="Seleccionado">
                <button mat-icon-button (click)="step.pictogramUrl = null" matTooltip="Quitar">
                  <mat-icon>close</mat-icon>
                </button>
              </div>
            </div>
          </div>

          <div class="step-actions">
            <button mat-icon-button (click)="subir(i)" [disabled]="i === 0" matTooltip="Subir">
              <mat-icon>arrow_upward</mat-icon>
            </button>
            <button mat-icon-button (click)="bajar(i)" [disabled]="i === steps.length - 1" matTooltip="Bajar">
              <mat-icon>arrow_downward</mat-icon>
            </button>
            <button mat-icon-button color="warn" (click)="eliminarPaso(i)" matTooltip="Eliminar paso">
              <mat-icon>delete</mat-icon>
            </button>
          </div>
        </div>

        <div class="empty-steps" *ngIf="steps.length === 0">
          <mat-icon>format_list_numbered</mat-icon>
          <p>Sin pasos. Añade el primero.</p>
        </div>

        <button mat-stroked-button class="btn-add-step" (click)="agregarPaso()">
          <mat-icon>add</mat-icon> Añadir paso
        </button>
      </div>

      <div class="cargando-wrap" *ngIf="cargando">
        <mat-spinner diameter="36"></mat-spinner>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button class="btn-save" (click)="guardar()" [disabled]="guardando || steps.length === 0">
        <mat-spinner diameter="16" *ngIf="guardando"></mat-spinner>
        <mat-icon *ngIf="!guardando">save</mat-icon>
        {{ guardando ? 'Guardando...' : 'Guardar pasos' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 8px; }

    .steps-content { min-width: 340px; max-height: 65vh; overflow-y: auto; padding: 8px 0; }

    .steps-list { display: flex; flex-direction: column; gap: 12px; }

    .step-card {
      display: flex; gap: 10px; align-items: flex-start;
      background: #F8F9FA; border-radius: 12px; padding: 12px;

      .step-num {
        min-width: 28px; height: 28px; border-radius: 50%;
        background: #A8D8EA; display: flex; align-items: center; justify-content: center;
        font-weight: 700; font-size: 0.85rem; flex-shrink: 0; margin-top: 8px;
      }

      .step-body { flex: 1; display: flex; flex-direction: column; gap: 6px; }

      .step-actions { display: flex; flex-direction: column; gap: 0; }
    }

    .full-width { width: 100%; }

    .image-mode-tabs { display: flex; gap: 6px; margin-bottom: 4px;
      .img-mode-btn {
        display: flex; align-items: center; gap: 4px; padding: 4px 10px;
        border: 1px solid #dee2e6; border-radius: 20px; background: none;
        font-size: 0.78rem; cursor: pointer; color: #6C757D;
        mat-icon { font-size: 14px; width: 14px; height: 14px; }
        &.active { background: #A8D8EA; border-color: #A8D8EA; color: #2C3E50; font-weight: 600; }
      }
    }

    .upload-placeholder {
      display: flex; align-items: center; justify-content: center;
      width: 64px; height: 64px; border: 2px dashed #dee2e6; border-radius: 8px;
      cursor: pointer; color: #adb5bd;
      mat-icon { font-size: 28px; }
    }

    .img-preview-wrap { position: relative; display: inline-flex;
      .step-img-preview { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; }
      button { position: absolute; top: -8px; right: -8px; width: 22px; height: 22px;
               --mdc-icon-button-state-layer-size: 22px;
               mat-icon { font-size: 14px; width: 14px; height: 14px; } }
    }

    .arasaac-mini { display: flex; flex-direction: column; gap: 6px;
      .arasaac-search-row { display: flex; gap: 4px;
        .arasaac-input { flex: 1; padding: 6px 10px; border: 1px solid #dee2e6;
                         border-radius: 8px; font-size: 0.82rem; }
      }
      .arasaac-grid-mini { display: grid; grid-template-columns: repeat(6, 1fr); gap: 4px;
        .arasaac-thumb { border: 2px solid transparent; border-radius: 6px; padding: 2px;
                         background: none; cursor: pointer;
                         img { width: 100%; aspect-ratio: 1; object-fit: contain; }
                         &.selected { border-color: #A8D8EA; }
        }
      }
      .selected-pictogram { display: flex; align-items: center; gap: 6px;
        img { width: 56px; height: 56px; object-fit: contain; border-radius: 8px;
              background: #f8f9fa; }
      }
    }

    .empty-steps { text-align: center; padding: 24px; color: #adb5bd;
      mat-icon { font-size: 2.5rem; width: 2.5rem; height: 2.5rem; } }

    .btn-add-step { width: 100%; border-radius: 8px !important; }

    .cargando-wrap { display: flex; justify-content: center; padding: 32px; }

    .btn-save { background: #A8D8EA !important; color: #2C3E50 !important;
                border-radius: var(--radius-md) !important; }
  `]
})
export class ActivityStepsDialogComponent implements OnInit {
  steps: StepDraft[] = [];
  cargando = true;
  guardando = false;

  constructor(
    private activityService: ActivityService,
    public arasaacService: ArasaacService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<ActivityStepsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ActivityStepsDialogData
  ) {}

  ngOnInit(): void {
    this.activityService.getSteps(this.data.activity.id).subscribe({
      next: (steps) => {
        this.steps = steps.map(s => ({
          title: s.title,
          description: s.description ?? '',
          imageBase64: s.imageBase64 ?? null,
          pictogramUrl: s.pictogramUrl ?? null,
          busquedaArasaac: '', resultadosArasaac: [], buscandoArasaac: false,
          modoImagen: s.pictogramUrl ? 'arasaac' : 'upload'
        }));
        this.cargando = false;
      },
      error: () => { this.cargando = false; }
    });
  }

  agregarPaso(): void {
    this.steps.push({
      title: '', description: '', imageBase64: null, pictogramUrl: null,
      busquedaArasaac: '', resultadosArasaac: [], buscandoArasaac: false, modoImagen: 'upload'
    });
  }

  eliminarPaso(i: number): void { this.steps.splice(i, 1); }

  subir(i: number): void {
    if (i === 0) return;
    [this.steps[i - 1], this.steps[i]] = [this.steps[i], this.steps[i - 1]];
  }

  bajar(i: number): void {
    if (i === this.steps.length - 1) return;
    [this.steps[i], this.steps[i + 1]] = [this.steps[i + 1], this.steps[i]];
  }

  alSeleccionarImagen(event: Event, step: StepDraft): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => { step.imageBase64 = reader.result as string; step.pictogramUrl = null; };
    reader.readAsDataURL(file);
  }

  buscarArasaac(step: StepDraft): void {
    if (!step.busquedaArasaac.trim()) return;
    step.buscandoArasaac = true;
    step.resultadosArasaac = [];
    this.arasaacService.search(step.busquedaArasaac.trim()).subscribe({
      next: (r) => { step.resultadosArasaac = r; step.buscandoArasaac = false; },
      error: () => { step.buscandoArasaac = false; }
    });
  }

  guardar(): void {
    const invalid = this.steps.find(s => !s.title.trim());
    if (invalid) {
      this.snackBar.open('Todos los pasos deben tener un título', 'Ok', { duration: 3000 });
      return;
    }
    this.guardando = true;
    const requests: ActivityStepRequest[] = this.steps.map(s => ({
      title: s.title.trim(),
      description: s.description || undefined,
      imageBase64: s.imageBase64 || undefined,
      pictogramUrl: s.pictogramUrl || undefined
    }));
    this.activityService.saveSteps(this.data.activity.id, requests).subscribe({
      next: () => {
        this.snackBar.open('Pasos guardados', 'Ok', { duration: 2000 });
        this.dialogRef.close(true);
      },
      error: () => { this.snackBar.open('Error al guardar', 'Cerrar', { duration: 3000 }); this.guardando = false; }
    });
  }
}
