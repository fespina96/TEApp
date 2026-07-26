import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AvatarPickerDialogComponent } from '../../../shared/components/avatar-picker-dialog/avatar-picker-dialog.component';
import { ChildService } from '../../../core/services/child.service';
import { Child } from '../../../core/models/child.model';

@Component({
  selector: 'app-child-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatTableModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MatDialogModule, MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './child-list.component.html',
  styleUrl: './child-list.component.scss'
})
export class ChildListComponent implements OnInit {
  participantes: Child[] = [];
  cargando = true;
  columnasVisibles = ['avatar', 'name', 'age', 'actions'];

  constructor(
    private childService: ChildService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cargarParticipantes();
  }

  cargarParticipantes(): void {
    this.cargando = true;
    this.childService.getAll().subscribe({
      next: (participantes) => { this.participantes = participantes; this.cargando = false; },
      error: () => { this.snackBar.open('Error al cargar perfiles', 'Cerrar', { duration: 3000 }); this.cargando = false; }
    });
  }

  eliminarParticipante(participante: Child): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `Eliminar a ${participante.name}`,
        message: `Esta acción eliminará el perfil y toda la agenda de ${participante.name}.`
      }
    });
    ref.afterClosed().subscribe(confirmado => {
      if (confirmado) this.childService.delete(participante.id).subscribe({
        next: () => { this.snackBar.open('Perfil eliminado', 'Ok', { duration: 2000 }); this.cargarParticipantes(); },
        error: () => this.snackBar.open('Error al eliminar', 'Cerrar', { duration: 3000 })
      });
    });
  }

  editarAvatar(participante: Child): void {
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

  calcularEdad(fechaNacimiento: string): number {
    const nacimiento = new Date(fechaNacimiento);
    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const meses = hoy.getMonth() - nacimiento.getMonth();
    if (meses < 0 || (meses === 0 && hoy.getDate() < nacimiento.getDate())) edad--;
    return edad;
  }
}
