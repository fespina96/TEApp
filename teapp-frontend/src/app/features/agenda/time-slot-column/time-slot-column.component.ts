import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { forkJoin } from 'rxjs';
import { ScheduleService } from '../../../core/services/schedule.service';
import { DayOfWeek, TimeSlot, ScheduleEntry, TIME_SLOT_LABELS, DAYS_OF_WEEK } from '../../../core/models/schedule-entry.model';
import { Activity } from '../../../core/models/activity.model';
import { EntryCardComponent } from '../entry-card/entry-card.component';
import { ActivityPickerDialogComponent } from '../activity-picker-dialog/activity-picker-dialog.component';

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
      maxHeight: '90vh'
    });

    dialogRef.afterClosed().subscribe((result?: {
      activity: Activity; agregarATodosLosDias: boolean;
      durationMinutes?: number; pausable?: boolean; requireFullTimer?: boolean;
    }) => {
      if (!result?.activity) return;

      const { activity, agregarATodosLosDias, durationMinutes, pausable, requireFullTimer } = result;
      const days = agregarATodosLosDias ? DAYS_OF_WEEK : [this.day];

      const requests = days.map(day =>
        this.scheduleService.addEntry(this.childId, {
          activityId: activity.id,
          dayOfWeek: day,
          timeSlot: this.slot,
          durationMinutes,
          pausable: pausable ?? true,
          requireFullTimer: requireFullTimer ?? false
        })
      );

      forkJoin(requests).subscribe({
        next: () => {
          const msg = agregarATodosLosDias ? 'Actividad agregada a todos los días' : 'Actividad agregada';
          this.snackBar.open(msg, 'Ok', { duration: 2500 });
          this.scheduleChanged.emit();
        },
        error: () => this.snackBar.open('Error al agregar la actividad', 'Cerrar', { duration: 3000 })
      });
    });
  }

  estaCompletadaHoy(entry: ScheduleEntry): boolean {
    const today = new Date().toISOString().split('T')[0];
    return entry.completedDates?.includes(today) ?? false;
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
