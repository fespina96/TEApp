import { Component, Inject, OnInit, Optional } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivityService } from '../../../core/services/activity.service';
import { Activity, ActivityCategory, CATEGORY_LABELS, CATEGORY_COLORS } from '../../../core/models/activity.model';

interface ActivityFormData {
  activity?: Activity;
}

@Component({
  selector: 'app-activity-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule,
    MatSlideToggleModule, MatSnackBarModule
  ],
  templateUrl: './activity-form.component.html',
  styleUrl: './activity-form.component.scss'
})
export class ActivityFormComponent implements OnInit {
  form: FormGroup;
  modoEdicion = false;
  cargando = false;

  readonly categoryLabels = CATEGORY_LABELS;
  readonly categories: ActivityCategory[] = [
    'HYGIENE', 'MEAL', 'EDUCATION', 'PLAY', 'THERAPY', 'REST', 'OUTDOOR', 'SPECIAL_EVENT', 'CUSTOM'
  ];

  readonly MATERIAL_ICONS = [
    'mood', 'bathtub', 'clean_hands', 'face', 'checkroom',
    'free_breakfast', 'restaurant', 'coffee', 'dinner_dining', 'local_drink',
    'menu_book', 'school', 'palette', 'extension', 'music_note',
    'toys', 'view_in_ar', 'tv', 'casino', 'sports_esports',
    'accessibility_new', 'record_voice_over', 'psychology', 'spa', 'self_improvement',
    'bedtime', 'nights_stay', 'park', 'directions_bike', 'yard', 'pool',
    'favorite', 'star', 'cake', 'pets', 'brush'
  ];

  constructor(
    private fb: FormBuilder,
    private activityService: ActivityService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<ActivityFormComponent>,
    @Optional() @Inject(MAT_DIALOG_DATA) public data: ActivityFormData
  ) {
    this.form = this.fb.group({
      name:            ['', [Validators.required, Validators.maxLength(150)]],
      description:     [''],
      category:        ['CUSTOM', Validators.required],
      iconName:        ['toys'],
      color:           ['#F9D8C0'],
      durationMinutes: [null],
      pausable:        [true]
    });
  }

  ngOnInit(): void {
    if (this.data?.activity) {
      this.modoEdicion = true;
      const a = this.data.activity;
      this.form.patchValue({
        name: a.name, description: a.description,
        category: a.category, iconName: a.iconName, color: a.color,
        durationMinutes: a.durationMinutes ?? null,
        pausable: a.pausable ?? true
      });
    }
    // Actualizar color al cambiar categoría
    this.form.get('category')?.valueChanges.subscribe((cat: ActivityCategory) => {
      if (!this.modoEdicion) {
        this.form.patchValue({ color: CATEGORY_COLORS[cat] });
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.cargando = true;
    const request = this.form.value;

    const op = this.modoEdicion && this.data.activity
      ? this.activityService.update(this.data.activity.id, request)
      : this.activityService.create(request);

    op.subscribe({
      next: () => {
        this.snackBar.open(this.modoEdicion ? 'Actividad actualizada' : 'Actividad creada', 'Ok', { duration: 2000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al guardar', 'Cerrar', { duration: 4000 });
        this.cargando = false;
      }
    });
  }
}
