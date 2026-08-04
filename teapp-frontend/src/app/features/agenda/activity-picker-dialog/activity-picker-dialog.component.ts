import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
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
import {
  DayOfWeek, TimeSlot, DAYS_OF_WEEK, DAY_SHORT_LABELS, TIME_SLOTS, TIME_SLOT_LABELS
} from '../../../core/models/schedule-entry.model';
import { ActivityChipComponent } from '../../../shared/components/activity-chip/activity-chip.component';

/** Día y franja desde donde se abrió el selector: quedan marcados de entrada. */
export interface DatosSelectorActividad {
  day: DayOfWeek;
  slot: TimeSlot;
}

/** Lo que devuelve el diálogo: la actividad y en qué días y franjas ubicarla. */
export interface ResultadoSelectorActividad {
  activity: Activity;
  dias: DayOfWeek[];
  franjas: TimeSlot[];
  durationMinutes?: number;
  pausable?: boolean;
  requireFullTimer?: boolean;
}

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
  /** La del catálogo que quedó marcada, a la espera del botón "Agregar". */
  actividadSeleccionada: Activity | null = null;

  // Destino: en qué días y en qué momentos del día se agrega, ambos de selección múltiple
  readonly dias = DAYS_OF_WEEK;
  readonly diaLabels = DAY_SHORT_LABELS;
  readonly franjas = TIME_SLOTS;
  readonly franjaLabels = TIME_SLOT_LABELS;
  diasElegidos = new Set<DayOfWeek>();
  franjasElegidas = new Set<TimeSlot>();

  // Los tres selectores arrancan plegados: el encabezado ya dice qué hay elegido
  mostrarCategorias = false;
  mostrarDias = false;
  mostrarFranjas = false;

  // Tabs
  pestanaActiva: 'catalog' | 'new' = 'catalog';

  // Nueva actividad
  newForm!: FormGroup;
  guardando = false;
  iconoSeleccionado = 'star';
  colorSeleccionado = '#F9D8C0';
  imagenSeleccionada: string | null = null;
  urlPictogramaSeleccionado: string | null = null;

  // Temporizador de entrada: las opciones sólo se muestran si está activado
  usaTemporizador = false;
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
    private fb: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public datos: DatosSelectorActividad
  ) {
    this.diasElegidos.add(datos.day);
    this.franjasElegidas.add(datos.slot);
  }

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

  get resumenCategoria(): string {
    return this.categoriaSeleccionada ? this.categoryLabels[this.categoriaSeleccionada] : 'Todas';
  }

  get resumenDias(): string {
    const elegidos = DAYS_OF_WEEK.filter(d => this.diasElegidos.has(d));
    return elegidos.length ? elegidos.map(d => DAY_SHORT_LABELS[d]).join(', ') : 'Ninguno';
  }

  get resumenFranjas(): string {
    const elegidas = TIME_SLOTS.filter(f => this.franjasElegidas.has(f));
    return elegidas.length ? elegidas.map(f => TIME_SLOT_LABELS[f]).join(', ') : 'Ninguno';
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

    // Si la marcada dejó de estar en pantalla se desmarca: agregar algo que no se
    // ve sería una sorpresa.
    if (this.actividadSeleccionada
        && !filtered.some(a => a.id === this.actividadSeleccionada!.id)) {
      this.actividadSeleccionada = null;
    }
  }

  /** Al apagarlo se descartan los valores, así no viajan en la entrada. */
  alCambiarTemporizador(): void {
    if (this.usaTemporizador) return;
    this.duracionEntrada = null;
    this.entradaPausable = true;
    this.entradaRequiereTemporizador = false;
  }

  // Destino
  alternarDia(dia: DayOfWeek): void {
    this.diasElegidos.has(dia) ? this.diasElegidos.delete(dia) : this.diasElegidos.add(dia);
  }

  alternarFranja(franja: TimeSlot): void {
    this.franjasElegidas.has(franja) ? this.franjasElegidas.delete(franja) : this.franjasElegidas.add(franja);
  }

  /** Hace falta al menos un día y un momento del día. */
  get destinoValido(): boolean {
    return this.diasElegidos.size > 0 && this.franjasElegidas.size > 0;
  }

  /** Se crea una entrada por cada combinación de día y franja. */
  get cantidadEntradas(): number {
    return this.diasElegidos.size * this.franjasElegidas.size;
  }

  /** Tocar una actividad sólo la marca; se agrega con el botón de abajo. */
  seleccionarActividad(activity: Activity): void {
    this.actividadSeleccionada =
      this.actividadSeleccionada?.id === activity.id ? null : activity;
  }

  agregarSeleccionada(): void {
    if (!this.actividadSeleccionada || !this.destinoValido) return;
    this.dialogRef.close(this.resultado(this.actividadSeleccionada));
  }

  /**
   * Se recorren las constantes y no los conjuntos para que el orden sea siempre
   * el de la semana, sin depender de en qué orden se hizo clic.
   */
  private resultado(activity: Activity): ResultadoSelectorActividad {
    return {
      activity,
      dias:    DAYS_OF_WEEK.filter(d => this.diasElegidos.has(d)),
      franjas: TIME_SLOTS.filter(f => this.franjasElegidas.has(f)),
      durationMinutes: this.duracionEntrada || undefined,
      pausable: this.entradaPausable,
      requireFullTimer: this.entradaRequiereTemporizador
    };
  }

  // Nueva actividad
  crearYSeleccionar(): void {
    if (this.newForm.invalid || !this.destinoValido) return;
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
      next: (activity) => this.dialogRef.close(this.resultado(activity)),
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
