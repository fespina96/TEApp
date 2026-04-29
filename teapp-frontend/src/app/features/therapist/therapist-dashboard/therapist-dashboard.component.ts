import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { TherapistService, SupervisedParent } from '../../../core/services/therapist.service';
import { AuthService } from '../../../core/services/auth.service';
import { DAY_LABELS, TIME_SLOT_LABELS, DAYS_OF_WEEK, TIME_SLOTS } from '../../../core/models/schedule-entry.model';

/**
 * Panel principal del terapeuta.
 * Permite visualizar los padres supervisados, sus participantes y sus agendas en modo solo lectura.
 */
@Component({
  selector: 'app-therapist-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTabsModule,
    MatTooltipModule,
    MatDividerModule
  ],
  templateUrl: './therapist-dashboard.component.html',
  styleUrl: './therapist-dashboard.component.scss'
})
export class TherapistDashboardComponent implements OnInit {
  /** Código de invitación del terapeuta para compartir con padres */
  codigoInvitacion = '';
  /** Lista de padres supervisados por el terapeuta */
  padres: SupervisedParent[] = [];
  /** Indica si se está cargando la lista de padres */
  cargando = true;

  /** Padre actualmente seleccionado */
  padreSeleccionado: SupervisedParent | null = null;
  /** Participantes del padre seleccionado */
  participantesDePadre: any[] = [];
  /** Participante actualmente seleccionado */
  participanteSeleccionado: any | null = null;
  /** Agenda del participante seleccionado */
  agendaParticipante: any | null = null;
  /** Indica si se están cargando los participantes del padre */
  cargandoParticipantes = false;
  /** Indica si se está cargando la agenda del participante */
  cargandoAgenda = false;

  readonly days = DAYS_OF_WEEK;
  readonly dayLabels = DAY_LABELS;
  readonly timeSlots = TIME_SLOTS;
  readonly slotLabels = TIME_SLOT_LABELS;

  constructor(
    private therapistService: TherapistService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const usuario = this.authService.currentUser;
    this.codigoInvitacion = usuario?.inviteCode ?? '';
    this.cargarPadres();
  }

  /** Carga la lista de padres supervisados desde el backend */
  cargarPadres(): void {
    this.cargando = true;
    this.therapistService.getSupervisedParents().subscribe({
      next: (padres) => { this.padres = padres; this.cargando = false; },
      error: () => { this.cargando = false; }
    });
  }

  /**
   * Selecciona o deselecciona un padre y carga sus participantes.
   * @param padre padre supervisado a seleccionar
   */
  seleccionarPadre(padre: SupervisedParent): void {
    if (this.padreSeleccionado?.id === padre.id) {
      this.padreSeleccionado = null;
      this.participantesDePadre = [];
      this.participanteSeleccionado = null;
      this.agendaParticipante = null;
      return;
    }
    this.padreSeleccionado = padre;
    this.participanteSeleccionado = null;
    this.agendaParticipante = null;
    this.cargandoParticipantes = true;
    this.therapistService.getSupervisedChildren(padre.id).subscribe({
      next: (participantes) => { this.participantesDePadre = participantes; this.cargandoParticipantes = false; },
      error: () => { this.cargandoParticipantes = false; }
    });
  }

  /**
   * Selecciona o deselecciona un participante y carga su agenda.
   * @param participante participante a seleccionar
   */
  seleccionarParticipante(participante: any): void {
    if (this.participanteSeleccionado?.id === participante.id) {
      this.participanteSeleccionado = null;
      this.agendaParticipante = null;
      return;
    }
    this.participanteSeleccionado = participante;
    this.cargandoAgenda = true;
    this.therapistService.getSupervisedSchedule(this.padreSeleccionado!.id, participante.id).subscribe({
      next: (agenda) => { this.agendaParticipante = agenda; this.cargandoAgenda = false; },
      error: () => { this.cargandoAgenda = false; }
    });
  }

  /**
   * Obtiene las entradas de un día y franja horaria de la agenda del participante seleccionado.
   * @param dia día de la semana
   * @param franja franja horaria
   */
  obtenerEntradas(dia: string, franja: string): any[] {
    return this.agendaParticipante?.week?.[dia]?.[franja] ?? [];
  }

  /** Copia el código de invitación al portapapeles */
  copiarCodigo(): void {
    navigator.clipboard.writeText(this.codigoInvitacion).then(() => {
      this.snackBar.open('Código copiado', '', { duration: 2000 });
    });
  }

  /**
   * Retorna la inicial del nombre en mayúscula.
   * @param nombre nombre completo
   */
  obtenerIniciales(nombre: string): string {
    return nombre.charAt(0).toUpperCase();
  }

  /**
   * Retorna el estilo de fondo del avatar de un participante.
   * @param participante participante del que se obtiene el color
   */
  estiloAvatar(participante: any): { [key: string]: string } {
    return { background: participante.avatarColor || '#A8D8EA' };
  }

  /** Retorna la fecha de hoy en formato ISO (YYYY-MM-DD) */
  fechaHoyISO(): string {
    return new Date().toISOString().split('T')[0];
  }

  /**
   * Retorna true si la entrada fue completada hoy.
   * @param entrada entrada de agenda a verificar
   */
  estaCompletadaHoy(entrada: any): boolean {
    return entrada.completedDates?.includes(this.fechaHoyISO()) ?? false;
  }
}
