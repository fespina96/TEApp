import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ScheduleEntry, ScheduleEntryRequest } from '../../../core/models/schedule-entry.model';

@Component({
  selector: 'app-entry-settings-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatButtonModule,
    MatIconModule, MatInputModule, MatFormFieldModule, MatSlideToggleModule
  ],
  template: `
    <h2 mat-dialog-title class="titulo">
      {{ entry.activity.name }}
    </h2>

    <mat-dialog-content class="contenido">
      <mat-form-field appearance="outline" class="campo">
        <mat-label>Notas</mat-label>
        <textarea matInput [(ngModel)]="notas" maxlength="500" rows="2"
                  placeholder="Ej: después del almuerzo"></textarea>
      </mat-form-field>

      <mat-slide-toggle [(ngModel)]="usaTemporizador" (change)="alCambiarTemporizador()"
                        color="primary" class="temporizador-switch">
        <mat-icon>timer</mat-icon>
        <span>Temporizador</span>
      </mat-slide-toggle>

      <div class="temporizador-opciones" *ngIf="usaTemporizador">
        <mat-form-field appearance="outline" class="campo">
          <mat-label>Duración (min)</mat-label>
          <input matInput type="number" [(ngModel)]="duracion" min="1" max="180"
                 placeholder="Ej: 15">
        </mat-form-field>

        <mat-slide-toggle [(ngModel)]="pausable" color="primary" [disabled]="!duracion">
          Pausable
        </mat-slide-toggle>

        <mat-slide-toggle [(ngModel)]="requiereTemporizador" color="primary" [disabled]="!duracion">
          Debe esperar que termine el conteo
        </mat-slide-toggle>
      </div>

      <p class="error" *ngIf="!duracionValida">
        La duración tiene que estar entre 1 y 180 minutos.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions class="acciones">
      <button mat-button [mat-dialog-close]="undefined">Cancelar</button>
      <button mat-flat-button color="primary" (click)="guardar()" [disabled]="!duracionValida">
        Guardar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .titulo { font-size: 1.05rem; }

    .contenido {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding-top: 8px !important;
    }

    .campo {
      width: 100%;
      ::ng-deep .mat-mdc-form-field-subscript-wrapper { display: none; }
    }

    .temporizador-switch {
      font-size: 0.8rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.4px;

      mat-icon {
        font-size: 15px;
        width: 15px;
        height: 15px;
        vertical-align: -2px;
        margin-right: 4px;
      }
    }

    .temporizador-opciones {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .error {
      margin: 0;
      font-size: 0.76rem;
      font-weight: 600;
      color: #B25C00;
    }

    .acciones {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  `]
})
export class EntrySettingsDialogComponent {
  entry: ScheduleEntry;

  notas: string;
  usaTemporizador: boolean;
  duracion: number | null;
  pausable: boolean;
  requiereTemporizador: boolean;

  constructor(
    private dialogRef: MatDialogRef<EntrySettingsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) datos: { entry: ScheduleEntry }
  ) {
    this.entry = datos.entry;
    this.notas = datos.entry.notes ?? '';
    this.duracion = datos.entry.durationMinutes ?? null;
    this.usaTemporizador = !!this.duracion;
    this.pausable = datos.entry.pausable ?? true;
    this.requiereTemporizador = datos.entry.requireFullTimer ?? false;
  }

  alCambiarTemporizador(): void {
    if (this.usaTemporizador) return;
    this.duracion = null;
    this.pausable = true;
    this.requiereTemporizador = false;
  }

  get duracionValida(): boolean {
    if (!this.usaTemporizador || !this.duracion) return true;
    return this.duracion >= 1 && this.duracion <= 180;
  }

  guardar(): void {
    if (!this.duracionValida) return;
    this.dialogRef.close({
      notes: this.notas.trim(),
      // 0 le pide al backend que quite la duración; un null lo dejaría como estaba.
      durationMinutes: this.usaTemporizador ? (this.duracion ?? 0) : 0,
      pausable: this.pausable,
      requireFullTimer: this.requiereTemporizador
    } as Partial<ScheduleEntryRequest>);
  }
}
