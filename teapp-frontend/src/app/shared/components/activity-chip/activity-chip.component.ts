import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Activity } from '../../../core/models/activity.model';

/**
 * Componente presentacional tipo chip/etiqueta para una actividad.
 * Muestra el icono de Material Icons y el nombre sobre el color de la actividad.
 */
@Component({
  selector: 'app-activity-chip',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <span class="activity-chip"
          [style.background-color]="activity.color"
          [attr.aria-label]="activity.name">
      <mat-icon *ngIf="activity.iconName">{{ activity.iconName }}</mat-icon>
      <span class="chip-label">{{ activity.name }}</span>
    </span>
  `,
  styles: [`
    .activity-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 0.83rem;
      font-weight: 600;
      color: #343A40;
      mat-icon { font-size: 16px; height: 16px; width: 16px; }
    }
  `]
})
export class ActivityChipComponent {
  /** Actividad a mostrar como chip */
  @Input({ required: true }) activity!: Activity;
}
