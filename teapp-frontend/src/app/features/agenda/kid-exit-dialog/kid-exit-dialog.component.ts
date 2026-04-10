import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Diálogo que solicita la contraseña del padre para salir del modo niño.
 */
@Component({
  selector: 'app-kid-exit-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Salir del modo participante</h2>
    <mat-dialog-content>
      <p class="dialog-hint">Ingresa tu contraseña para volver a la vista de administración.</p>
      <form [formGroup]="form" (ngSubmit)="confirm()">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Contraseña</mat-label>
          <input matInput [type]="hide ? 'password' : 'text'"
                 formControlName="password" autocomplete="current-password">
          <button mat-icon-button matSuffix type="button" (click)="hide = !hide"
                  aria-label="Mostrar contraseña">
            <mat-icon>{{ hide ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        </mat-form-field>
        <p class="error-msg" *ngIf="errorMsg">{{ errorMsg }}</p>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button color="primary" (click)="confirm()"
              [disabled]="form.invalid || checking">
        {{ checking ? 'Verificando...' : 'Confirmar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-hint { color: #666; font-size: 0.9rem; margin-bottom: 16px; }
    .full-width { width: 100%; }
    .error-msg { color: #c0392b; font-size: 0.85rem; margin: 4px 0 0; }
  `]
})
export class KidExitDialogComponent {
  form: FormGroup;
  hide = true;
  checking = false;
  errorMsg = '';

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<KidExitDialogComponent>,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      password: ['', Validators.required]
    });
  }

  confirm(): void {
    if (this.form.invalid) return;
    this.checking = true;
    this.errorMsg = '';

    const email = this.authService.currentUser?.email ?? '';
    this.authService.login({ email, password: this.form.value.password }).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.errorMsg = 'Contraseña incorrecta';
        this.checking = false;
      }
    });
  }
}
