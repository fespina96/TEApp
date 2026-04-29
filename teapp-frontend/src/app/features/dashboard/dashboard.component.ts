import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChildCardComponent } from '../../shared/components/child-card/child-card.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { AvatarPickerDialogComponent } from '../../shared/components/avatar-picker-dialog/avatar-picker-dialog.component';
import { ChildService } from '../../core/services/child.service';
import { AuthService } from '../../core/services/auth.service';
import { TherapistService, LinkedTherapist } from '../../core/services/therapist.service';
import { Child } from '../../core/models/child.model';

/**
 * Dashboard principal del padre.
 * Muestra la lista de participantes registrados con acceso rápido a sus agendas
 * y gestión de la vinculación con terapeutas.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    ChildCardComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  /** Lista de participantes del padre autenticado */
  participantes: Child[] = [];
  /** Indica si se está cargando la lista de participantes */
  cargando = true;

  /** Lista de terapeutas vinculados al padre */
  terapeutas: LinkedTherapist[] = [];
  /** Controla la visibilidad del formulario de vinculación */
  mostrarFormVinculacion = false;
  /** Código de invitación ingresado por el padre */
  codigoTerapeuta = '';

  /** Retorna true si el usuario autenticado es padre (no terapeuta) */
  get esPadre(): boolean {
    return this.authService.currentUser?.role !== 'THERAPIST';
  }

  constructor(
    private childService: ChildService,
    private authService: AuthService,
    private therapistService: TherapistService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cargarParticipantes();
    if (this.esPadre) {
      this.cargarTerapeutas();
    }
  }

  /** Carga la lista de participantes desde el backend */
  cargarParticipantes(): void {
    this.cargando = true;
    this.childService.getAll().subscribe({
      next: (participantes) => {
        this.participantes = participantes;
        this.cargando = false;
      },
      error: () => {
        this.snackBar.open('Error al cargar los perfiles', 'Cerrar', { duration: 3000 });
        this.cargando = false;
      }
    });
  }

  /** Navega al formulario de creación de un nuevo participante */
  agregarParticipante(): void {
    this.router.navigate(['/app/children/new']);
  }

  /**
   * Navega al formulario de edición de un participante.
   * @param participante participante a editar
   */
  editarParticipante(participante: Child): void {
    this.router.navigate(['/app/children', participante.id, 'edit']);
  }

  /**
   * Abre el diálogo para cambiar el avatar de un participante.
   * @param participante participante cuyo avatar se va a editar
   */
  editarAvatarParticipante(participante: Child): void {
    const ref = this.dialog.open(AvatarPickerDialogComponent, {
      width: '380px',
      data: { name: participante.name, currentAvatar: participante.avatarBase64 }
    });
    ref.afterClosed().subscribe((resultado: string | null | undefined) => {
      if (resultado === undefined) return;
      const avatar = resultado ?? '';
      this.childService.updateAvatar(participante.id, avatar).subscribe({
        next: () => {
          participante.avatarBase64 = avatar || undefined;
          this.snackBar.open('Foto actualizada', 'Ok', { duration: 2500 });
        },
        error: () => this.snackBar.open('Error al guardar la foto', 'Cerrar', { duration: 3000 })
      });
    });
  }

  /**
   * Muestra confirmación y elimina el perfil de un participante.
   * @param participante participante a eliminar
   */
  eliminarParticipante(participante: Child): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `Eliminar perfil de ${participante.name}`,
        message: `¿Estás seguro? Se eliminará el perfil y toda la agenda de ${participante.name}. Esta acción no se puede deshacer.`,
        confirmLabel: 'Eliminar'
      }
    });

    ref.afterClosed().subscribe(confirmado => {
      if (confirmado) {
        this.childService.delete(participante.id).subscribe({
          next: () => {
            this.snackBar.open(`Perfil de ${participante.name} eliminado`, 'Ok', { duration: 3000 });
            this.cargarParticipantes();
          },
          error: () => this.snackBar.open('Error al eliminar el perfil', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  /** Carga la lista de terapeutas vinculados al padre */
  cargarTerapeutas(): void {
    this.therapistService.getMyTherapists().subscribe({
      next: (lista) => this.terapeutas = lista,
      error: () => {}
    });
  }

  /** Vincula al padre con el terapeuta usando el código ingresado */
  vincularTerapeuta(): void {
    const codigo = this.codigoTerapeuta.trim();
    if (!codigo) return;
    this.therapistService.linkToTherapist(codigo).subscribe({
      next: () => {
        this.snackBar.open('Vinculado correctamente', 'Ok', { duration: 3000 });
        this.codigoTerapeuta = '';
        this.mostrarFormVinculacion = false;
        this.cargarTerapeutas();
      },
      error: (err) => {
        const mensaje = err.error?.message || 'Código inválido o error al vincular';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      }
    });
  }

  /**
   * Desvincula al padre de un terapeuta específico previa confirmación.
   * @param idTerapeuta identificador del terapeuta a desvincular
   */
  desvincularTerapeuta(idTerapeuta: string): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Desvincular terapeuta',
        message: '¿Estás seguro de que querés desvincular a este terapeuta? Ya no podrá ver ni modificar actividades en las agendas de tus participantes.',
        confirmLabel: 'Desvincular'
      }
    });
    ref.afterClosed().subscribe(confirmado => {
      if (!confirmado) return;
      this.therapistService.unlinkFromTherapist(idTerapeuta).subscribe({
        next: () => {
          this.snackBar.open('Desvinculado correctamente', 'Ok', { duration: 3000 });
          this.cargarTerapeutas();
        },
        error: () => this.snackBar.open('Error al desvincular', 'Cerrar', { duration: 3000 })
      });
    });
  }

  /** Retorna el primer nombre del usuario autenticado */
  get nombreUsuario(): string {
    return this.authService.currentUser?.fullName?.split(' ')[0] || 'Papá/Mamá';
  }
}
