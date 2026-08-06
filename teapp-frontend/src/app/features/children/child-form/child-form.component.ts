import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { concatMap, of } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ChildService } from '../../../core/services/child.service';
import { AVATAR_COLORS } from '../../../core/models/child.model';
import { fechaISOLocal, fechaMaximaNacimiento } from '../../../core/utils/fecha.util';
import { recolorearAvatar } from '../../../core/utils/avatar-emoji.util';

// El modo (crear vs editar) se detecta según la presencia del parámetro de ruta idParticipante.
@Component({
  selector: 'app-child-form',
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
    MatSnackBarModule
  ],
  templateUrl: './child-form.component.html',
  styleUrl: './child-form.component.scss'
})
export class ChildFormComponent implements OnInit {
  form: FormGroup;
  modoEdicion = false;
  idParticipante: string | null = null;
  cargando = false;
  fechaMaxima = fechaMaximaNacimiento();
  coloresAvatar = AVATAR_COLORS;
  avatarBase64?: string;

  private avatarOriginal?: string;

  constructor(
    private fb: FormBuilder,
    private childService: ChildService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      name:        ['', [Validators.required, Validators.maxLength(100)]],
      dateOfBirth: [null, Validators.required],
      avatarColor: ['#A8D8EA'],
      notes:       ['']
    });
  }

  ngOnInit(): void {
    // El nombre del parámetro tiene que coincidir con el de app.routes.ts.
    this.idParticipante = this.route.snapshot.paramMap.get('childId');
    this.modoEdicion = !!this.idParticipante;

    if (this.modoEdicion && this.idParticipante) {
      this.childService.getById(this.idParticipante).subscribe({
        next: (child) => {
          this.avatarBase64 = child.avatarBase64;
          this.avatarOriginal = child.avatarBase64;
          this.form.patchValue({
            name:        child.name,
            dateOfBirth: new Date(child.dateOfBirth),
            avatarColor: child.avatarColor,
            notes:       child.notes
          });
        },
        error: () => {
          this.snackBar.open('Error al cargar el perfil', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/app/children']);
        }
      });
    }
  }

  /**
   * El color del perfil es el fondo del avatar. En los del catálogo ese color va
   * dentro del SVG, así que hay que regenerarlo para que el cambio se vea y se
   * guarde; una foto subida se deja como está.
   */
  seleccionarColor(color: string): void {
    this.form.patchValue({ avatarColor: color });
    this.avatarBase64 = recolorearAvatar(this.avatarBase64, color);
  }

  enviar(): void {
    if (this.form.invalid) return;
    this.cargando = true;

    const value = this.form.value;
    const request = {
      name:        value.name,
      dateOfBirth: fechaISOLocal(value.dateOfBirth as Date),
      avatarColor: value.avatarColor,
      notes:       value.notes || undefined
    };

    const operation = this.modoEdicion && this.idParticipante
      ? this.childService.update(this.idParticipante, request)
      : this.childService.create(request);

    // El avatar viaja por su propio endpoint, así que si cambió de color se
    // guarda a continuación del perfil.
    operation.pipe(
      concatMap((child) => this.avatarBase64 && this.avatarBase64 !== this.avatarOriginal
        ? this.childService.updateAvatar(child.id, this.avatarBase64)
        : of(void 0))
    ).subscribe({
      next: () => {
        const msg = this.modoEdicion ? 'Perfil actualizado' : 'Perfil creado';
        this.snackBar.open(msg, 'Ok', { duration: 3000 });
        this.router.navigate(['/app/dashboard']);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al guardar el perfil', 'Cerrar', { duration: 4000 });
        this.cargando = false;
      }
    });
  }
}
