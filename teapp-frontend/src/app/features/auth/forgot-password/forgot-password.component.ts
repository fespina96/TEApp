import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  form: FormGroup;
  cargando = false;
  enviado = false;
  mensajeError = '';

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  enviar(): void {
    if (this.form.invalid) return;
    this.cargando = true;
    this.mensajeError = '';

    this.authService.forgotPassword(this.form.value.email).subscribe({
      next: () => {
        this.enviado = true;
        this.cargando = false;
      },
      error: (err) => {
        if (err.status === 0) {
          // Error de red real: el servidor no responde
          this.mensajeError = 'No se pudo conectar con el servidor. Verificá tu conexión.';
          this.cargando = false;
        } else {
          // Cualquier otra respuesta HTTP: mostrar éxito por seguridad
          // (evita revelar si el email existe o no)
          this.enviado = true;
          this.cargando = false;
        }
      }
    });
  }
}
