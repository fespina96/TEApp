import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { forkJoin } from 'rxjs';
import { ScheduleService } from '../../../core/services/schedule.service';
import {
  DayOfWeek, TimeSlot, ScheduleEntry, ScheduleEntryRequest, TIME_SLOT_LABELS
} from '../../../core/models/schedule-entry.model';
import { EntryCardComponent } from '../entry-card/entry-card.component';
import {
  ActivityPickerDialogComponent, ResultadoSelectorActividad
} from '../activity-picker-dialog/activity-picker-dialog.component';
import { EntrySettingsDialogComponent } from '../entry-settings-dialog/entry-settings-dialog.component';
import { fechaISOLocal } from '../../../core/utils/fecha.util';

@Component({
  selector: 'app-time-slot-column',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule, MatIconModule, MatDialogModule, MatSnackBarModule,
    DragDropModule,
    EntryCardComponent
  ],
  templateUrl: './time-slot-column.component.html',
  styleUrl: './time-slot-column.component.scss'
})
export class TimeSlotColumnComponent implements OnChanges {
  @Input({ required: true }) childId!: string;
  @Input({ required: true }) day!: DayOfWeek;
  @Input({ required: true }) slot!: TimeSlot;
  @Input() entries: ScheduleEntry[] = [];

  // Copia mutable local para reordenar por drag-and-drop sin recargar del backend.
  entradasLocales: ScheduleEntry[] = [];

  @Output() scheduleChanged = new EventEmitter<void>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['entries']) {
      this.entradasLocales = [...this.entries];
    }
  }

  readonly etiquetasFranja = TIME_SLOT_LABELS;

  readonly iconosFranja: Record<TimeSlot, string> = {
    MORNING:   'wb_sunny',
    AFTERNOON: 'wb_cloudy',
    NIGHT:     'nights_stay'
  };

  constructor(
    private scheduleService: ScheduleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  get claseFranja(): string {
    const map: Record<TimeSlot, string> = {
      MORNING: 'slot-morning',
      AFTERNOON: 'slot-afternoon',
      NIGHT: 'slot-night'
    };
    return map[this.slot];
  }

  alSoltar(event: CdkDragDrop<ScheduleEntry[]>): void {
    if (event.previousContainer !== event.container) return;
    if (event.previousIndex === event.currentIndex) return;

    const backup = [...this.entradasLocales];
    moveItemInArray(this.entradasLocales, event.previousIndex, event.currentIndex);

    const requests = this.entradasLocales.map((entry, idx) =>
      this.scheduleService.updateEntry(this.childId, entry.id, {
        activityId: entry.activity.id,
        dayOfWeek:  this.day,
        timeSlot:   this.slot,
        sortOrder:  idx
      })
    );

    forkJoin(requests).subscribe({
      error: () => {
        this.entradasLocales = backup;
        this.snackBar.open('Error al reordenar', 'Cerrar', { duration: 3000 });
      }
    });
  }

  abrirSelectorActividad(): void {
    const dialogRef = this.dialog.open(ActivityPickerDialogComponent, {
      width: '480px',
      maxWidth: '96vw',
      // Alto fijo, con aire arriba y abajo: si dependiera del contenido, el
      // diálogo cambiaría de tamaño cada vez que se filtra el catálogo.
      height: '86vh',
      panelClass: 'picker-dialog-panel',
      // El día y la franja de esta columna quedan marcados en el diálogo.
      data: { day: this.day, slot: this.slot }
    });

    dialogRef.afterClosed().subscribe((result?: ResultadoSelectorActividad) => {
      if (!result?.activity) return;

      const { activity, dias, franjas, durationMinutes, pausable, requireFullTimer } = result;

      // Una entrada por cada combinación de día y franja elegidos.
      const requests = dias.flatMap(day =>
        franjas.map(slot =>
          this.scheduleService.addEntry(this.childId, {
            activityId: activity.id,
            dayOfWeek: day,
            timeSlot: slot,
            durationMinutes,
            pausable: pausable ?? true,
            requireFullTimer: requireFullTimer ?? false
          })
        )
      );

      forkJoin(requests).subscribe({
        next: () => {
          const msg = requests.length === 1
            ? 'Actividad agregada'
            : `Se agregaron ${requests.length} actividades`;
          this.snackBar.open(msg, 'Ok', { duration: 2500 });
          this.scheduleChanged.emit();
        },
        error: () => this.snackBar.open('Error al agregar la actividad', 'Cerrar', { duration: 3000 })
      });
    });
  }

  estaCompletadaHoy(entry: ScheduleEntry): boolean {
    const today = fechaISOLocal();
    return entry.completedDates?.includes(today) ?? false;
  }

  /** Edita una entrada ya cargada: notas y temporizador. El resto no cambia. */
  alEditarEntrada(entry: ScheduleEntry): void {
    const dialogRef = this.dialog.open(EntrySettingsDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data: { entry }
    });

    dialogRef.afterClosed().subscribe((cambios?: Partial<ScheduleEntryRequest>) => {
      if (!cambios) return;
      this.scheduleService.updateEntry(this.childId, entry.id, cambios).subscribe({
        next: () => {
          this.snackBar.open('Actividad actualizada', 'Ok', { duration: 2000 });
          this.scheduleChanged.emit();
        },
        error: () => this.snackBar.open('Error al actualizar', 'Cerrar', { duration: 3000 })
      });
    });
  }

  alEliminarEntrada(entry: ScheduleEntry): void {
    this.scheduleService.deleteEntry(this.childId, entry.id).subscribe({
      next: () => {
        this.snackBar.open('Actividad eliminada', 'Ok', { duration: 2000 });
        this.scheduleChanged.emit();
      },
      error: () => this.snackBar.open('Error al eliminar', 'Cerrar', { duration: 3000 })
    });
  }
}
