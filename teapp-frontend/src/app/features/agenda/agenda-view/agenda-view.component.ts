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

/**
 * Vista principal de la agenda semanal de un participante.
 * Incluye modo participante: vista de hoy, solo lectura, con marca de tareas completadas.
 */
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
  /** Perfil del participante cuya agenda se está viendo */
  participante: Child | null = null;
  /** Agenda semanal completa */
  agenda: WeeklySchedule | null = null;
  /** Indica si se está cargando la información */
  cargando = true;
  /** ID del participante obtenido de los parámetros de ruta */
  idParticipante!: string;

  /** Modo participante: solo muestra las tareas de hoy, sin edición */
  modoNino = false;
  /** Información del clima para el modo participante */
  clima: WeatherInfo | null = null;

  readonly days = DAYS_OF_WEEK;
  readonly dayLabels = DAY_LABELS;
  readonly timeSlots = TIME_SLOTS;
  readonly slotLabels = TIME_SLOT_LABELS;

  /** Íconos de Material asociados a cada franja horaria */
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

  /** Carga el perfil del participante y luego su agenda */
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

  /** Carga la agenda semanal del participante desde el backend */
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

  /**
   * Obtiene las entradas de un día y franja horaria específicos.
   * @param dia día de la semana
   * @param franja franja horaria (MORNING, AFTERNOON, NIGHT)
   */
  obtenerEntradas(dia: DayOfWeek, franja: TimeSlot): ScheduleEntry[] {
    return this.agenda?.week?.[dia]?.[franja] ?? [];
  }

  /** Recarga la agenda tras agregar o eliminar una entrada */
  alCambiarAgenda(): void {
    this.cargarAgenda();
  }

  /** Índice del día actual para el tab seleccionado por defecto */
  get indiceDiaActual(): number {
    const mapaDias: Record<number, number> = { 1: 0, 2: 1, 3: 2, 4: 3, 5: 4, 6: 5, 0: 6 };
    return mapaDias[new Date().getDay()] ?? 0;
  }

  /** Día de la semana actual como DayOfWeek */
  get diaActual(): DayOfWeek {
    return this.days[this.indiceDiaActual];
  }

  /** Iniciales del nombre del participante para el avatar */
  get inicialesParticipante(): string {
    return this.participante?.name.charAt(0).toUpperCase() ?? '?';
  }

  /** Retorna true si hoy es el cumpleaños del participante */
  get esCumpleanos(): boolean {
    if (!this.participante?.dateOfBirth) return false;
    const hoy = new Date();
    const nacimiento = new Date(this.participante.dateOfBirth + 'T00:00:00');
    return nacimiento.getMonth() === hoy.getMonth() && nacimiento.getDate() === hoy.getDate();
  }

  /** Edad del participante calculada desde su fecha de nacimiento */
  get edadParticipante(): number | null {
    if (!this.participante?.dateOfBirth) return null;
    const hoy = new Date();
    const nacimiento = new Date(this.participante.dateOfBirth + 'T00:00:00');
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const meses = hoy.getMonth() - nacimiento.getMonth();
    if (meses < 0 || (meses === 0 && hoy.getDate() < nacimiento.getDate())) edad--;
    return edad;
  }

  /** Activa el modo participante y carga el clima si aún no se cargó */
  activarModoNino(): void {
    this.modoNino = true;
    if (!this.clima) {
      this.weatherService.getWeather().subscribe(info => this.clima = info);
    }
  }

  /** Solicita contraseña para salir del modo participante */
  salirModoNino(): void {
    this.dialog.open(KidExitDialogComponent, { width: '360px', disableClose: true })
      .afterClosed().subscribe((confirmado: boolean) => {
        if (confirmado) this.modoNino = false;
      });
  }

  /**
   * Alterna el estado completado de una entrada.
   * Si la actividad tiene pasos, muestra el visor de pasos primero.
   * Si tiene temporizador, lo muestra antes de marcar completado.
   * @param entrada entrada de agenda a marcar/desmarcar
   */
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

  /**
   * Retorna true si la entrada fue completada hoy.
   * @param entrada entrada de agenda a verificar
   */
  estaCompletadaHoy(entrada: ScheduleEntry): boolean {
    return entrada.completedDates?.includes(this.fechaHoyISO()) ?? false;
  }

  /** Resetea todas las completitudes de la semana actual */
  resetearSemana(): void {
    this.scheduleService.resetCurrentWeek(this.idParticipante).subscribe({
      next: () => {
        this.snackBar.open('Semana reseteada', 'Ok', { duration: 2500 });
        this.cargarAgenda();
      }
    });
  }

  /** Retorna la fecha de hoy en formato ISO (YYYY-MM-DD) */
  private fechaHoyISO(): string {
    return new Date().toISOString().split('T')[0];
  }

  /**
   * Retorna las entradas de hoy para una franja horaria específica.
   * @param franja franja horaria a consultar
   */
  obtenerEntradasHoy(franja: TimeSlot): ScheduleEntry[] {
    return this.obtenerEntradas(this.diaActual, franja);
  }

  /** Todas las entradas de hoy en orden de franja horaria */
  get todasEntradasHoy(): ScheduleEntry[] {
    return TIME_SLOTS.flatMap(franja => this.obtenerEntradasHoy(franja));
  }

  /** Primera actividad pendiente de hoy (Ahora) */
  get entradaActual(): ScheduleEntry | null {
    return this.todasEntradasHoy.find(e => !this.estaCompletadaHoy(e)) ?? null;
  }

  /** Segunda actividad pendiente de hoy (Después) */
  get entradaSiguiente(): ScheduleEntry | null {
    const pendientes = this.todasEntradasHoy.filter(e => !this.estaCompletadaHoy(e));
    return pendientes[1] ?? null;
  }
}
