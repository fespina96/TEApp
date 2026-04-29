import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../../core/services/user.service';

function passwordsMatch(control: AbstractControl) {
  const form = control as FormGroup;
  const np = form.get('newPassword')?.value;
  const cp = form.get('confirmPassword')?.value;
  return np === cp ? null : { passwordsMismatch: true };
}

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './change-password-dialog.component.html',
  styleUrl: './change-password-dialog.component.scss'
})
export class ChangePasswordDialogComponent {
  form: FormGroup;
  cargando = false;
  exitoso = false;
  mensajeError = '';
  ocultarActual = true;
  ocultarNueva = true;
  ocultarConfirmacion = true;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ChangePasswordDialogComponent>,
    private userService: UserService
  ) {
    this.form = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/)
      ]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: passwordsMatch });
  }

  onSubmit(): void {
    if (this.form.invalid || this.cargando) return;
    this.cargando = true;
    this.mensajeError = '';

    const { currentPassword, newPassword } = this.form.value;
    this.userService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.cargando = false;
        this.exitoso = true;
        setTimeout(() => this.dialogRef.close(true), 2000);
      },
      error: (err) => {
        this.cargando = false;
        this.mensajeError = err.error?.message || 'Error al cambiar la contraseña';
      }
    });
  }

  cancelar(): void {
    this.dialogRef.close(false);
  }
}
