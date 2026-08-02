import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivityService } from '../../../core/services/activity.service';
import { ArasaacService, ArasaacPictogram } from '../../../core/services/arasaac.service';
import { Activity, ActivityCategory, ActivityRequest, CATEGORY_LABELS, CATEGORY_COLORS } from '../../../core/models/activity.model';
import { ActivityChipComponent } from '../../../shared/components/activity-chip/activity-chip.component';

const ICON_OPTIONS = [
  'star', 'brush', 'sports_soccer', 'directions_run', 'restaurant',
  'self_improvement', 'music_note', 'book', 'local_hospital', 'spa',
  'nightlight', 'wb_sunny', 'shopping_bag', 'pets', 'directions_bike',
  'pool', 'emoji_events', 'face', 'home', 'school'
];

@Component({
  selector: 'app-activity-picker-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatInputModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatSlideToggleModule, MatSelectModule,
    MatTooltipModule, ActivityChipComponent
  ],
  templateUrl: './activity-picker-dialog.component.html',
  styleUrl: './activity-picker-dialog.component.scss'
})
export class ActivityPickerDialogComponent implements OnInit {
  // Catálogo
  activities: Activity[] = [];
  actividadesFiltradas: Activity[] = [];
  predefiniadasFiltradas: Activity[] = [];
  terapeutaFiltradas: Activity[] = [];
  personalizadasFiltradas: Activity[] = [];
  cargando = true;
  busqueda = '';
  categoriaSeleccionada: ActivityCategory | null = null;
  agregarATodosLosDias = false;

  // Tabs
  pestanaActiva: 'catalog' | 'new' = 'catalog';

  // Nueva actividad
  newForm!: FormGroup;
  guardando = false;
  iconoSeleccionado = 'star';
  colorSeleccionado = '#F9D8C0';
  imagenSeleccionada: string | null = null;
  urlPictogramaSeleccionado: string | null = null;

  // Temporizador de entrada
  duracionEntrada: number | null = null;
  entradaPausable = true;
  entradaRequiereTemporizador = false;

  // ARASAAC
  modoImagen: 'upload' | 'arasaac' = 'upload';
  busquedaArasaac = '';
  resultadosArasaac: ArasaacPictogram[] = [];
  buscandoArasaac = false;

  readonly categoryLabels = CATEGORY_LABELS;
  readonly categoryColors = CATEGORY_COLORS;
  readonly categories: ActivityCategory[] = [
    'HYGIENE', 'MEAL', 'EDUCATION', 'PLAY', 'THERAPY', 'REST', 'OUTDOOR', 'SPECIAL_EVENT', 'CUSTOM'
  ];
  readonly iconOptions = ICON_OPTIONS;

  readonly colorPresets = [
    '#A8D8EA', '#FAF0BE', '#B8E0C8', '#C9B8E8',
    '#D4E8C8', '#E0D8F0', '#C8E8D0', '#F9D8C0',
    '#FFD6A5', '#CAFFBF', '#BDE0FE', '#FFC8DD'
  ];

  constructor(
    private activityService: ActivityService,
    public arasaacService: ArasaacService,
    public dialogRef: MatDialogRef<ActivityPickerDialogComponent>,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.newForm = this.fb.group({
      name:            ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      category:        ['CUSTOM' as ActivityCategory, Validators.required],
      durationMinutes: [null, [Validators.min(1), Validators.max(180)]],
      pausable:        [true]
    });

    this.newForm.get('category')!.valueChanges.subscribe((cat: ActivityCategory) => {
      this.colorSeleccionado = CATEGORY_COLORS[cat] ?? '#F9D8C0';
    });

    this.activityService.getAvailable().subscribe({
      next: (activities) => {
        this.activities = activities;
        this.aplicarFiltro();
        this.cargando = false;
      },
      error: () => { this.cargando = false; }
    });
  }

  // Catálogo
  seleccionarCategoria(cat: ActivityCategory | null): void {
    this.categoriaSeleccionada = cat;
    this.aplicarFiltro();
  }

  alBuscar(): void { this.aplicarFiltro(); }

  aplicarFiltro(): void {
    const filtered = this.activities.filter(a => {
      const matchesCat    = !this.categoriaSeleccionada || a.category === this.categoriaSeleccionada;
      const matchesSearch = !this.busqueda ||
                             a.name.toLowerCase().includes(this.busqueda.toLowerCase());
      return matchesCat && matchesSearch;
    });
    this.predefiniadasFiltradas = filtered.filter(a => a.predefined);
    this.terapeutaFiltradas  = filtered.filter(a => a.therapistCreated);
    this.personalizadasFiltradas     = filtered.filter(a => !a.predefined && !a.therapistCreated);
    this.actividadesFiltradas = filtered;
  }

  seleccionarActividad(activity: Activity): void {
    this.dialogRef.close({
      activity,
      agregarATodosLosDias: this.agregarATodosLosDias,
      durationMinutes: this.duracionEntrada || undefined,
      pausable: this.entradaPausable,
      requireFullTimer: this.entradaRequiereTemporizador
    });
  }

  // Nueva actividad
  crearYSeleccionar(): void {
    if (this.newForm.invalid) return;
    this.guardando = true;

    const request: ActivityRequest = {
      name:            this.newForm.value.name.trim(),
      category:        this.newForm.value.category,
      iconName:        this.iconoSeleccionado,
      color:           this.colorSeleccionado,
      imageBase64:     this.imagenSeleccionada ?? undefined,
      pictogramUrl:    this.urlPictogramaSeleccionado ?? undefined,
      durationMinutes: this.newForm.value.durationMinutes || undefined,
      pausable:        this.newForm.value.pausable ?? true
    };

    this.activityService.create(request).subscribe({
      next: (activity) => {
        this.dialogRef.close({
          activity,
          agregarATodosLosDias: this.agregarATodosLosDias,
          durationMinutes: this.duracionEntrada || undefined,
          pausable: this.entradaPausable,
          requireFullTimer: this.entradaRequiereTemporizador
        });
      },
      error: () => { this.guardando = false; }
    });
  }

  alSeleccionarImagen(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.imagenSeleccionada = reader.result as string;
      this.urlPictogramaSeleccionado = null;
    };
    reader.readAsDataURL(file);
  }

  quitarImagen(): void {
    this.imagenSeleccionada = null;
    this.urlPictogramaSeleccionado = null;
  }

  // ARASAAC
  buscarArasaac(): void {
    if (!this.busquedaArasaac.trim()) return;
    this.buscandoArasaac = true;
    this.resultadosArasaac = [];
    this.arasaacService.search(this.busquedaArasaac.trim()).subscribe({
      next: (results) => {
        this.resultadosArasaac = results;
        this.buscandoArasaac = false;
      },
      error: () => { this.buscandoArasaac = false; }
    });
  }

  seleccionarPictograma(pictogram: ArasaacPictogram): void {
    this.urlPictogramaSeleccionado = this.arasaacService.imageUrl(pictogram._id);
    this.imagenSeleccionada = null;
  }

}
