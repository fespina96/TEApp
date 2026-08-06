import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { AuthService } from '../../../core/services/auth.service';
import { fechaISOLocal, fechaMaximaNacimiento } from '../../../core/utils/fecha.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCheckboxModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  loginForm: FormGroup;
  registerForm: FormGroup;
  cargando = false;
  mensajeError = '';
  mensajeExito = '';
  ocultarContrasena = true;
  ocultarConfirmacion = true;
  rolSeleccionado: 'PARENT' | 'THERAPIST' = 'PARENT';
  fechaMaxima = fechaMaximaNacimiento();
  pestanaActiva: 'login' | 'register' = 'login';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email:      ['', [Validators.required, Validators.email]],
      password:   ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });

    this.registerForm = this.fb.group({
      fullName:        ['', [Validators.required, Validators.maxLength(255)]],
      email:           ['', [Validators.required, Validators.email]],
      dateOfBirth:     [null, [Validators.required]],
      password:        ['', [Validators.required, Validators.minLength(8),
                             Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.validadorContrasenasIguales });
  }

  private validadorContrasenasIguales(grupo: AbstractControl): ValidationErrors | null {
    const pw  = grupo.get('password')?.value;
    const cpw = grupo.get('confirmPassword')?.value;
    return pw && cpw && pw !== cpw ? { passwordsMismatch: true } : null;
  }

  iniciarSesion(): void {
    if (this.loginForm.invalid) return;
    this.cargando = true;
    this.mensajeError = '';
    this.mensajeExito = '';

    const { rememberMe, ...credenciales } = this.loginForm.value;
    this.authService.login(credenciales, rememberMe).subscribe({
      next: (respuesta) => {
        const destino = respuesta.role === 'THERAPIST' ? '/app/therapist' : '/app/dashboard';
        this.router.navigate([destino]);
      },
      error: (err) => {
        if (err.status === 0) {
          this.mensajeError = 'No se pudo conectar con el servidor. Verificá tu conexión.';
        } else if (err.status === 401) {
          this.mensajeError = 'Email o contraseña incorrectos.';
        } else {
          this.mensajeError = 'Error al iniciar sesión. Intentá de nuevo.';
        }
        this.cargando = false;
      }
    });
  }

  registrar(): void {
    if (this.registerForm.invalid) return;
    this.cargando = true;
    this.mensajeError = '';
    this.mensajeExito = '';

    const { confirmPassword, dateOfBirth, ...datosRegistro } = this.registerForm.value;
    const fechaNacimiento = dateOfBirth instanceof Date
      ? fechaISOLocal(dateOfBirth)
      : dateOfBirth;
    this.authService.register({ ...datosRegistro, dateOfBirth: fechaNacimiento, role: this.rolSeleccionado }).subscribe({
      next: () => {
        // No se entra automáticamente: se vuelve al login con el email ya puesto.
        const email = this.registerForm.value.email;
        this.registerForm.reset();
        this.loginForm.patchValue({ email });
        this.pestanaActiva = 'login';
        this.mensajeError = '';
        this.mensajeExito = 'Cuenta creada correctamente. Ingresá con tu email y contraseña.';
        this.cargando = false;
      },
      error: (err) => {
        this.mensajeError = err.error?.message || 'Error al registrarse. Intenta de nuevo.';
        this.cargando = false;
      }
    });
  }
}
