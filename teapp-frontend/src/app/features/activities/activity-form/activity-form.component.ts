import { Component, Inject, OnInit, Optional } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivityService } from '../../../core/services/activity.service';
import { ArasaacService, ArasaacPictogram } from '../../../core/services/arasaac.service';
import { Activity, ActivityCategory, CATEGORY_LABELS, CATEGORY_COLORS } from '../../../core/models/activity.model';

interface ActivityFormData {
  activity?: Activity;
}

@Component({
  selector: 'app-activity-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule, ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule,
    MatSlideToggleModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './activity-form.component.html',
  styleUrl: './activity-form.component.scss'
})
export class ActivityFormComponent implements OnInit {
  form: FormGroup;
  modoEdicion = false;
  cargando = false;

  // Imagen de la actividad: subida propia o pictograma de ARASAAC
  modoImagen: 'upload' | 'arasaac' = 'arasaac';
  /** Lo que se muestra y se envía: un data URI o la URL del pictograma. */
  imagenElegida: string | null = null;
  esPictograma = false;
  busquedaArasaac = '';
  resultadosArasaac: ArasaacPictogram[] = [];
  buscandoArasaac = false;

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
    public arasaac: ArasaacService,
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
      durationMinutes: [null, [Validators.min(1), Validators.max(180)]],
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
      // La actividad puede traer un pictograma o una imagen propia, nunca las dos.
      this.imagenElegida = a.pictogramUrl ?? a.imageBase64 ?? null;
      this.esPictograma  = !!a.pictogramUrl;
      this.modoImagen    = a.imageBase64 && !a.pictogramUrl ? 'upload' : 'arasaac';
    }
    // Actualizar color al cambiar categoría
    this.form.get('category')?.valueChanges.subscribe((cat: ActivityCategory) => {
      if (!this.modoEdicion) {
        this.form.patchValue({ color: CATEGORY_COLORS[cat] });
      }
    });
  }

  alSeleccionarImagen(event: Event): void {
    const archivo = (event.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    const lector = new FileReader();
    lector.onload = () => {
      this.imagenElegida = lector.result as string;
      this.esPictograma = false;
    };
    lector.readAsDataURL(archivo);
  }

  buscarArasaac(): void {
    const termino = this.busquedaArasaac.trim();
    if (!termino) return;
    this.buscandoArasaac = true;
    this.resultadosArasaac = [];
    this.arasaac.search(termino).subscribe({
      next: (resultados) => {
        this.resultadosArasaac = resultados;
        this.buscandoArasaac = false;
      },
      error: () => {
        this.buscandoArasaac = false;
        this.snackBar.open('No se pudo buscar en ARASAAC', 'Cerrar', { duration: 3000 });
      }
    });
  }

  elegirPictograma(picto: ArasaacPictogram): void {
    this.imagenElegida = this.arasaac.imageUrl(picto._id);
    this.esPictograma = true;
  }

  quitarImagen(): void {
    this.imagenElegida = null;
    this.esPictograma = false;
  }

  /** URL del pictograma en un campo, imagen propia en el otro: el backend los guarda por separado. */
  get urlPictograma(): string | null {
    return this.esPictograma ? this.imagenElegida : null;
  }

  enviar(): void {
    if (this.form.invalid) return;
    this.cargando = true;
    const request = {
      ...this.form.value,
      pictogramUrl: this.esPictograma ? this.imagenElegida ?? undefined : undefined,
      imageBase64:  this.esPictograma ? undefined : this.imagenElegida ?? undefined
    };

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
