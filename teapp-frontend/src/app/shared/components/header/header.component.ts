import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { User } from '../../../core/models/user.model';
import { AvatarPickerDialogComponent } from '../avatar-picker-dialog/avatar-picker-dialog.component';
import { ChangePasswordDialogComponent } from '../change-password-dialog/change-password-dialog.component';

/**
 * Barra de navegación superior de la aplicación.
 * Muestra el logo, navegación principal y menú de usuario con opciones de perfil.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {
  /** Usuario autenticado actualmente */
  usuarioActual: User | null = null;

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(usuario => this.usuarioActual = usuario);
  }

  /** Cierra la sesión del usuario actual */
  logout(): void {
    this.authService.logout();
  }

  /** Abre el diálogo para cambiar la contraseña del usuario */
  abrirCambioContrasena(): void {
    const ref = this.dialog.open(ChangePasswordDialogComponent, {
      width: '420px',
      maxWidth: '96vw'
    });
    ref.afterClosed().subscribe(cambiada => {
      if (cambiada) {
        this.snackBar.open('Contraseña actualizada correctamente', 'Ok', { duration: 3000 });
      }
    });
  }

  /** Abre el selector de avatar para actualizar la foto de perfil del usuario */
  abrirSelectorAvatar(): void {
    const ref = this.dialog.open(AvatarPickerDialogComponent, {
      width: '380px',
      data: { name: this.usuarioActual?.fullName || 'Usuario', currentAvatar: this.usuarioActual?.avatarBase64 }
    });
    ref.afterClosed().subscribe((resultado: string | null | undefined) => {
      if (resultado === undefined) return;
      const avatar = resultado ?? '';
      this.userService.updateAvatar(avatar).subscribe({
        next: () => {
          this.authService.updateLocalAvatar(avatar || null);
          this.snackBar.open('Foto actualizada', 'Ok', { duration: 2500 });
        },
        error: () => this.snackBar.open('Error al guardar la foto', 'Cerrar', { duration: 3000 })
      });
    });
  }

  /** Iniciales del nombre del usuario para el avatar */
  get iniciales(): string {
    if (!this.usuarioActual?.fullName) return '?';
    return this.usuarioActual.fullName
      .split(' ')
      .slice(0, 2)
      .map(n => n[0])
      .join('')
      .toUpperCase();
  }
}
