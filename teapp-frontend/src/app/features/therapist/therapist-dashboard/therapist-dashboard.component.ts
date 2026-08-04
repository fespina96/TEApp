import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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
import { fechaISOLocal } from '../../../core/utils/fecha.util';

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
  codigoInvitacion = '';
  padres: SupervisedParent[] = [];
  cargando = true;

  padreSeleccionado: SupervisedParent | null = null;
  participantesDePadre: any[] = [];
  participanteSeleccionado: any | null = null;
  agendaParticipante: any | null = null;
  cargandoParticipantes = false;
  cargandoAgenda = false;

  readonly dias = DAYS_OF_WEEK;
  readonly etiquetasDia = DAY_LABELS;
  readonly franjas = TIME_SLOTS;
  readonly etiquetasFranja = TIME_SLOT_LABELS;

  constructor(
    private therapistService: TherapistService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  /** Abre la agenda del participante supervisado en la misma vista que usa el padre. */
  gestionarAgenda(): void {
    if (!this.participanteSeleccionado) return;
    // La ruta cuelga de 'app': sin ese prefijo no matchea ninguna y no pasa nada.
    this.router.navigate(['/app/children', this.participanteSeleccionado.id, 'agenda']);
  }

  ngOnInit(): void {
    const usuario = this.authService.currentUser;
    this.codigoInvitacion = usuario?.inviteCode ?? '';
    this.cargarPadres();
  }

  cargarPadres(): void {
    this.cargando = true;
    this.therapistService.getSupervisedParents().subscribe({
      next: (padres) => { this.padres = padres; this.cargando = false; },
      error: () => { this.cargando = false; }
    });
  }

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

  obtenerEntradas(dia: string, franja: string): any[] {
    return this.agendaParticipante?.week?.[dia]?.[franja] ?? [];
  }

  copiarCodigo(): void {
    navigator.clipboard.writeText(this.codigoInvitacion).then(() => {
      this.snackBar.open('Código copiado', '', { duration: 2000 });
    });
  }

  obtenerIniciales(nombre: string): string {
    return nombre.charAt(0).toUpperCase();
  }

  estiloAvatar(participante: any): { [key: string]: string } {
    return { background: participante.avatarColor || '#A8D8EA' };
  }

  fechaHoyISO(): string {
    return fechaISOLocal();
  }

  estaCompletadaHoy(entrada: any): boolean {
    return entrada.completedDates?.includes(this.fechaHoyISO()) ?? false;
  }
}
