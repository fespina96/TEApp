import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ScheduleService } from '../../../core/services/schedule.service';
import { ChildService } from '../../../core/services/child.service';
import { SpeechService } from '../../../core/services/speech.service';
import { WeatherService, WeatherInfo } from '../../../core/services/weather.service';
import { Child } from '../../../core/models/child.model';
import {
  DayOfWeek, TimeSlot, WeeklySchedule, ScheduleEntry,
  DAYS_OF_WEEK, DAY_LABELS, TIME_SLOTS, TIME_SLOT_LABELS
} from '../../../core/models/schedule-entry.model';
import { TimeSlotColumnComponent } from '../time-slot-column/time-slot-column.component';
import { KidExitDialogComponent } from '../kid-exit-dialog/kid-exit-dialog.component';
import { VisualTimerDialogComponent } from '../../../shared/components/visual-timer-dialog/visual-timer-dialog.component';
import { StepViewerDialogComponent } from '../../../shared/components/step-viewer-dialog/step-viewer-dialog.component';
import { ActivityService } from '../../../core/services/activity.service';
import { fechaISOLocal } from '../../../core/utils/fecha.util';

@Component({
  selector: 'app-agenda-view',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatTabsModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule,
    MatCheckboxModule, MatTooltipModule,
    TimeSlotColumnComponent
  ],
  templateUrl: './agenda-view.component.html',
  styleUrl: './agenda-view.component.scss'
})
export class AgendaViewComponent implements OnInit {
  participante: Child | null = null;
  agenda: WeeklySchedule | null = null;
  cargando = true;
  idParticipante!: string;

  modoNino = false;
  clima: WeatherInfo | null = null;

  readonly dias = DAYS_OF_WEEK;
  readonly etiquetasDia = DAY_LABELS;
  readonly franjas = TIME_SLOTS;
  readonly etiquetasFranja = TIME_SLOT_LABELS;

  readonly iconosFranja: Record<TimeSlot, string> = {
    MORNING:   'wb_sunny',
    AFTERNOON: 'wb_cloudy',
    NIGHT:     'nights_stay'
  };

  constructor(
    private route: ActivatedRoute,
    private scheduleService: ScheduleService,
    private childService: ChildService,
    private activityService: ActivityService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    public speech: SpeechService,
    private weatherService: WeatherService
  ) {}

  ngOnInit(): void {
    this.idParticipante = this.route.snapshot.paramMap.get('childId')!;
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.childService.getById(this.idParticipante).subscribe({
      next: (participante) => {
        this.participante = participante;
        this.cargarAgenda();
      },
      error: () => {
        this.snackBar.open('Error al cargar el perfil', 'Cerrar', { duration: 3000 });
        this.cargando = false;
      }
    });
  }

  cargarAgenda(): void {
    this.scheduleService.getWeeklySchedule(this.idParticipante).subscribe({
      next: (agenda) => {
        this.agenda = agenda;
        this.cargando = false;
      },
      error: () => {
        this.snackBar.open('Error al cargar la agenda', 'Cerrar', { duration: 3000 });
        this.cargando = false;
      }
    });
  }

  obtenerEntradas(dia: DayOfWeek, franja: TimeSlot): ScheduleEntry[] {
    return this.agenda?.week?.[dia]?.[franja] ?? [];
  }

  alCambiarAgenda(): void {
    this.cargarAgenda();
  }

  get indiceDiaActual(): number {
    return (new Date().getDay() + 6) % 7;
  }

  get diaActual(): DayOfWeek {
    return this.dias[this.indiceDiaActual];
  }

  get inicialesParticipante(): string {
    return this.participante?.name.charAt(0).toUpperCase() ?? '?';
  }

  get esCumpleanos(): boolean {
    if (!this.participante?.dateOfBirth) return false;
    const hoy = new Date();
    const nacimiento = new Date(this.participante.dateOfBirth + 'T00:00:00');
    return nacimiento.getMonth() === hoy.getMonth() && nacimiento.getDate() === hoy.getDate();
  }

  get edadParticipante(): number | null {
    if (!this.participante?.dateOfBirth) return null;
    const hoy = new Date();
    const nacimiento = new Date(this.participante.dateOfBirth + 'T00:00:00');
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const meses = hoy.getMonth() - nacimiento.getMonth();
    if (meses < 0 || (meses === 0 && hoy.getDate() < nacimiento.getDate())) edad--;
    return edad;
  }

  activarModoNino(): void {
    this.modoNino = true;
    if (!this.clima) {
      this.weatherService.getWeather().subscribe(info => this.clima = info);
    }
  }

  salirModoNino(): void {
    this.dialog.open(KidExitDialogComponent, { width: '360px', disableClose: true })
      .afterClosed().subscribe((confirmado: boolean) => {
        if (confirmado) this.modoNino = false;
      });
  }

  // Si la actividad tiene pasos o temporizador, se muestran antes de marcarla completada.
  alternarCompletada(entrada: ScheduleEntry): void {
    const fechaHoy = this.fechaHoyISO();
    if (this.estaCompletadaHoy(entrada)) {
      this.scheduleService.unmarkCompleted(this.idParticipante, entrada.id, fechaHoy)
        .subscribe({ next: () => this.cargarAgenda() });
      return;
    }

    if ((entrada.activity.stepCount ?? 0) > 0) {
      this.activityService.getSteps(entrada.activity.id).subscribe(pasos => {
        this.dialog.open(StepViewerDialogComponent, {
          data: {
            activityName:  entrada.activity.name,
            activityColor: entrada.activity.color,
            steps: pasos
          },
          maxWidth: '96vw',
          disableClose: true
        }).afterClosed().subscribe((completada: boolean) => {
          if (completada) {
            this.scheduleService.markCompleted(this.idParticipante, entrada.id, fechaHoy)
              .subscribe({ next: () => this.cargarAgenda() });
          }
        });
      });
      return;
    }

    if (entrada.durationMinutes) {
      this.dialog.open(VisualTimerDialogComponent, {
        data: {
          activityName:     entrada.activity.name,
          activityColor:    entrada.activity.color,
          pictogramUrl:     entrada.activity.pictogramUrl,
          imageBase64:      entrada.activity.imageBase64,
          durationMinutes:  entrada.durationMinutes,
          pausable:         entrada.pausable ?? true,
          requireFullTimer: entrada.requireFullTimer ?? false
        },
        disableClose: true,
        maxWidth: '96vw'
      }).afterClosed().subscribe((completada: boolean) => {
        if (completada) {
          this.scheduleService.markCompleted(this.idParticipante, entrada.id, fechaHoy)
            .subscribe({ next: () => this.cargarAgenda() });
        }
      });
      return;
    }

    this.scheduleService.markCompleted(this.idParticipante, entrada.id, fechaHoy)
      .subscribe({ next: () => this.cargarAgenda() });
  }

  estaCompletadaHoy(entrada: ScheduleEntry): boolean {
    return entrada.completedDates?.includes(this.fechaHoyISO()) ?? false;
  }

  resetearSemana(): void {
    this.scheduleService.resetCurrentWeek(this.idParticipante).subscribe({
      next: () => {
        this.snackBar.open('Semana reseteada', 'Ok', { duration: 2500 });
        this.cargarAgenda();
      }
    });
  }

  private fechaHoyISO(): string {
    return fechaISOLocal();
  }

  obtenerEntradasHoy(franja: TimeSlot): ScheduleEntry[] {
    return this.obtenerEntradas(this.diaActual, franja);
  }

  get todasEntradasHoy(): ScheduleEntry[] {
    return TIME_SLOTS.flatMap(franja => this.obtenerEntradasHoy(franja));
  }

  get entradaActual(): ScheduleEntry | null {
    return this.todasEntradasHoy.find(e => !this.estaCompletadaHoy(e)) ?? null;
  }

  get entradaSiguiente(): ScheduleEntry | null {
    const pendientes = this.todasEntradasHoy.filter(e => !this.estaCompletadaHoy(e));
    return pendientes[1] ?? null;
  }
}
