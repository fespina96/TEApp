import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { User } from '../../core/models/user.model';
import { AvatarPickerDialogComponent } from '../../shared/components/avatar-picker-dialog/avatar-picker-dialog.component';
import { ChangePasswordDialogComponent } from '../../shared/components/change-password-dialog/change-password-dialog.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

/**
 * Pantalla de perfil del usuario autenticado.
 * Permite editar nombre, fecha de nacimiento, avatar, contraseña
 * y eliminar la cuenta.
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatDatepickerModule, MatNativeDateModule,
    MatDividerModule, MatDialogModule,
    MatSnackBarModule, MatProgressSpinnerModule
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  usuario: User | null = null;
  form!: FormGroup;
  guardando = false;
  cargando = true;

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fullName:    ['', [Validators.required, Validators.maxLength(255)]],
      dateOfBirth: [null]
    });

    this.authService.currentUser$.subscribe(u => {
      this.usuario = u;
      if (u) {
        this.form.patchValue({
          fullName:    u.fullName,
          dateOfBirth: u.dateOfBirth ? new Date(u.dateOfBirth + 'T00:00:00') : null
        });
      }
    });

    // Cargar datos frescos del servidor
    this.userService.getMe().subscribe({
      next: res => {
        this.cargando = false;
        const user: User = {
          id:           res.id,
          email:        res.email,
          fullName:     res.fullName,
          avatarBase64: res.avatarBase64,
          role:         res.role,
          inviteCode:   res.inviteCode,
          dateOfBirth:  res.dateOfBirth,
          createdAt:    res.createdAt
        };
        this.usuario = user;
        this.form.patchValue({
          fullName:    user.fullName,
          dateOfBirth: user.dateOfBirth ? new Date(user.dateOfBirth + 'T00:00:00') : null
        });
        this.authService.updateLocalAvatar(user.avatarBase64 ?? null);
      },
      error: () => { this.cargando = false; }
    });
  }

  get iniciales(): string {
    if (!this.usuario?.fullName) return '?';
    return this.usuario.fullName.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase();
  }

  get fechaAlta(): string {
    if (!this.usuario?.createdAt) return '—';
    return new Date(this.usuario.createdAt).toLocaleDateString('es-AR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
  }

  get rolLabel(): string {
    return this.usuario?.role === 'THERAPIST' ? 'Terapeuta' : 'Padre / Tutor';
  }

  guardarPerfil(): void {
    if (this.form.invalid) return;
    this.guardando = true;
    const { fullName, dateOfBirth } = this.form.value;
    const dob = dateOfBirth ? (dateOfBirth as Date).toISOString().split('T')[0] : undefined;

    this.userService.updateProfile(fullName, dob).subscribe({
      next: res => {
        this.guardando = false;
        this.snackBar.open('Perfil actualizado', 'Ok', { duration: 2500 });
        const updated: User = {
          id:           res.id,
          email:        res.email,
          fullName:     res.fullName,
          avatarBase64: res.avatarBase64,
          role:         res.role,
          inviteCode:   res.inviteCode,
          dateOfBirth:  res.dateOfBirth,
          createdAt:    res.createdAt
        };
        this.usuario = updated;
        this.authService.updateLocalAvatar(updated.avatarBase64 ?? null);
      },
      error: () => {
        this.guardando = false;
        this.snackBar.open('Error al actualizar el perfil', 'Cerrar', { duration: 3000 });
      }
    });
  }

  abrirCambioAvatar(): void {
    const ref = this.dialog.open(AvatarPickerDialogComponent, {
      width: '380px',
      data: { name: this.usuario?.fullName ?? '', currentAvatar: this.usuario?.avatarBase64 }
    });
    ref.afterClosed().subscribe((resultado: string | null | undefined) => {
      if (resultado === undefined) return;
      const avatar = resultado ?? '';
      this.userService.updateAvatar(avatar).subscribe({
        next: () => {
          this.authService.updateLocalAvatar(avatar || null);
          if (this.usuario) this.usuario = { ...this.usuario, avatarBase64: avatar || undefined };
          this.snackBar.open('Foto actualizada', 'Ok', { duration: 2500 });
        },
        error: () => this.snackBar.open('Error al guardar la foto', 'Cerrar', { duration: 3000 })
      });
    });
  }

  abrirCambioContrasena(): void {
    const ref = this.dialog.open(ChangePasswordDialogComponent, { width: '420px', maxWidth: '96vw' });
    ref.afterClosed().subscribe(cambiada => {
      if (cambiada) this.snackBar.open('Contraseña actualizada', 'Ok', { duration: 2500 });
    });
  }

  confirmarEliminarCuenta(): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar cuenta',
        message: '¿Estás seguro? Esta acción es irreversible. Tu cuenta y todos tus datos serán eliminados permanentemente.'
      }
    });
    ref.afterClosed().subscribe(confirmado => {
      if (!confirmado) return;
      this.userService.deleteAccount().subscribe({
        next: () => {
          this.authService.logout();
          this.snackBar.open('Cuenta eliminada', 'Ok', { duration: 3000 });
        },
        error: () => this.snackBar.open('Error al eliminar la cuenta', 'Cerrar', { duration: 3000 })
      });
    });
  }
}
