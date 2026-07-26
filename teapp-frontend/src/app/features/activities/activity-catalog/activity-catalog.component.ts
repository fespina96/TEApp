import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivityService } from '../../../core/services/activity.service';
import { Activity, ActivityCategory, CATEGORY_LABELS } from '../../../core/models/activity.model';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ActivityChipComponent } from '../../../shared/components/activity-chip/activity-chip.component';
import { ActivityFormComponent } from '../activity-form/activity-form.component';
import { ActivityStepsDialogComponent } from '../../../shared/components/activity-steps-dialog/activity-steps-dialog.component';

@Component({
  selector: 'app-activity-catalog',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule, MatIconModule, MatChipsModule,
    MatProgressSpinnerModule, MatDialogModule, MatSnackBarModule,
    MatTooltipModule, ActivityChipComponent
  ],
  templateUrl: './activity-catalog.component.html',
  styleUrl: './activity-catalog.component.scss'
})
export class ActivityCatalogComponent implements OnInit {
  actividades: Activity[] = [];
  actividadesFiltradas: Activity[] = [];
  cargando = true;
  categoriaSeleccionada: ActivityCategory | null = null;

  readonly categoryLabels = CATEGORY_LABELS;
  readonly categories: ActivityCategory[] = [
    'HYGIENE', 'MEAL', 'EDUCATION', 'PLAY', 'THERAPY', 'REST', 'OUTDOOR', 'SPECIAL_EVENT', 'CUSTOM'
  ];

  constructor(
    private activityService: ActivityService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cargarActividades();
  }

  cargarActividades(): void {
    this.cargando = true;
    this.activityService.getAvailable().subscribe({
      next: (actividades) => {
        this.actividades = actividades;
        this.aplicarFiltro();
        this.cargando = false;
      },
      error: () => {
        this.snackBar.open('Error al cargar actividades', 'Cerrar', { duration: 3000 });
        this.cargando = false;
      }
    });
  }

  seleccionarCategoria(categoria: ActivityCategory | null): void {
    this.categoriaSeleccionada = categoria;
    this.aplicarFiltro();
  }

  aplicarFiltro(): void {
    this.actividadesFiltradas = this.categoriaSeleccionada
      ? this.actividades.filter(a => a.category === this.categoriaSeleccionada)
      : this.actividades;
  }

  abrirFormCrear(): void {
    const ref = this.dialog.open(ActivityFormComponent, {
      width: '500px',
      maxWidth: '96vw',
      maxHeight: '90vh'
    });
    ref.afterClosed().subscribe(creada => {
      if (creada) this.cargarActividades();
    });
  }

  editarActividad(actividad: Activity): void {
    const ref = this.dialog.open(ActivityFormComponent, {
      width: '500px',
      maxWidth: '96vw',
      maxHeight: '90vh',
      data: { activity: actividad }
    });
    ref.afterClosed().subscribe(actualizada => {
      if (actualizada) this.cargarActividades();
    });
  }

  gestionarPasos(actividad: Activity): void {
    this.dialog.open(ActivityStepsDialogComponent, {
      width: '520px', maxWidth: '96vw', maxHeight: '90vh',
      data: { activity: actividad }
    }).afterClosed().subscribe(guardado => {
      if (guardado) this.cargarActividades();
    });
  }

  eliminarActividad(actividad: Activity): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `Eliminar actividad`,
        message: `¿Eliminar "${actividad.name}"? Solo puedes eliminarla si no está siendo usada en ninguna agenda.`
      }
    });
    ref.afterClosed().subscribe(confirmado => {
      if (confirmado) {
        this.activityService.delete(actividad.id).subscribe({
          next: () => {
            this.snackBar.open('Actividad eliminada', 'Ok', { duration: 2000 });
            this.cargarActividades();
          },
          error: (err) => this.snackBar.open(err.error?.message || 'No se pudo eliminar', 'Cerrar', { duration: 4000 })
        });
      }
    });
  }
}
