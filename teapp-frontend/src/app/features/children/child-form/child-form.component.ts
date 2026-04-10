import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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

/**
 * Formulario de creación y edición de perfil de niño.
 * Detecta el modo (crear vs editar) según la presencia del parámetro childId en la ruta.
 */
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
  isEditMode = false;
  childId: string | null = null;
  loading = false;
  maxDate = new Date();
  avatarColors = AVATAR_COLORS;

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
    this.childId = this.route.snapshot.paramMap.get('childId');
    this.isEditMode = !!this.childId;

    if (this.isEditMode && this.childId) {
      this.childService.getById(this.childId).subscribe({
        next: (child) => {
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

  selectColor(color: string): void {
    this.form.patchValue({ avatarColor: color });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    const value = this.form.value;
    const request = {
      name:        value.name,
      dateOfBirth: (value.dateOfBirth as Date).toISOString().split('T')[0],
      avatarColor: value.avatarColor,
      notes:       value.notes || undefined
    };

    const operation = this.isEditMode && this.childId
      ? this.childService.update(this.childId, request)
      : this.childService.create(request);

    operation.subscribe({
      next: () => {
        const msg = this.isEditMode ? 'Perfil actualizado' : 'Perfil creado';
        this.snackBar.open(msg, 'Ok', { duration: 3000 });
        this.router.navigate(['/app/dashboard']);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al guardar el perfil', 'Cerrar', { duration: 4000 });
        this.loading = false;
      }
    });
  }
}
