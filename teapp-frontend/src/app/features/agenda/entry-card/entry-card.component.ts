import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivityChipComponent } from '../../../shared/components/activity-chip/activity-chip.component';
import { ScheduleEntry } from '../../../core/models/schedule-entry.model';

@Component({
  selector: 'app-entry-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule, ActivityChipComponent],
  template: `
    <div class="entry-card"
         [class.entry-done]="isCompletedToday"
         [class.entry-special]="entry.activity.category === 'SPECIAL_EVENT'">

      <div *ngIf="entry.activity.category === 'SPECIAL_EVENT'" class="special-banner">
        <mat-icon>star</mat-icon>
        <span>Evento especial</span>
        <mat-icon>star</mat-icon>
      </div>

      <app-activity-chip [activity]="entry.activity"></app-activity-chip>

      <mat-icon *ngIf="isCompletedToday" class="done-badge" matTooltip="Completada hoy">
        check_circle
      </mat-icon>

      <img *ngIf="entry.activity.imageBase64 || entry.activity.pictogramUrl"
           [src]="entry.activity.imageBase64 || entry.activity.pictogramUrl"
           class="activity-image"
           alt="">

      <div class="entry-meta" *ngIf="entry.startTime || entry.notes">
        <span *ngIf="entry.startTime" class="time-range">
          {{ entry.startTime }}{{ entry.endTime ? ' - ' + entry.endTime : '' }}
        </span>
        <span *ngIf="entry.notes" class="entry-note">{{ entry.notes }}</span>
      </div>

      <button mat-icon-button class="delete-btn"
              (click)="delete.emit(entry)"
              [matTooltip]="'Quitar ' + entry.activity.name"
              aria-label="Eliminar actividad de la agenda">
        <mat-icon>close</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .entry-card {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      background: rgba(255,255,255,0.7);
      border-radius: 14px;
      padding: 6px 4px 6px 8px;
      border: 1px solid rgba(255,255,255,0.9);
      transition: background 0.2s;

      &:hover { background: rgba(255,255,255,0.9); }
      &.entry-done { opacity: 0.65; }

      &.entry-special {
        background: linear-gradient(135deg, #FFFDE7 0%, #FFF8E1 100%);
        border: 2px solid #FFC107;
        box-shadow: 0 3px 12px rgba(255, 193, 7, 0.4);
        padding-top: 4px;
      }

      app-activity-chip { flex: 1; min-width: 0; }

      .special-banner {
        display: flex;
        align-items: center;
        gap: 4px;
        width: 100%;
        flex-basis: 100%;
        order: -1;
        font-size: 0.7rem;
        font-weight: 700;
        color: #E65100;
        letter-spacing: 0.5px;
        text-transform: uppercase;
        animation: special-pulse 2s ease-in-out infinite;

        mat-icon {
          font-size: 13px; width: 13px; height: 13px;
          color: #FFC107;
        }
      }

      @keyframes special-pulse {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.7; }
      }

      .done-badge {
        font-size: 18px; width: 18px; height: 18px;
        color: #27AE60; flex-shrink: 0;
      }

      .activity-image {
        width: 100%;
        height: 72px;
        object-fit: cover;
        border-radius: 8px;
        margin-top: 2px;
        display: block;
        order: 10;
        flex-basis: 100%;
      }

      .entry-meta {
        display: flex;
        flex-direction: column;
        font-size: 0.72rem;
        color: #6C757D;
        .time-range { font-weight: 600; }
        .entry-note { font-style: italic; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 80px; }
      }

      .delete-btn {
        flex-shrink: 0;
        width: 28px !important;
        height: 28px !important;
        --mdc-icon-button-state-layer-size: 28px;
        display: inline-flex !important;
        align-items: center !important;
        justify-content: center !important;
        padding: 0 !important;
        mat-icon { font-size: 16px; height: 16px; width: 16px; color: rgba(0,0,0,0.35); }
        &:hover mat-icon { color: #c0392b; }
      }
    }
  `]
})
export class EntryCardComponent {
  @Input({ required: true }) entry!: ScheduleEntry;
  @Input() isCompletedToday = false;
  @Output() delete = new EventEmitter<ScheduleEntry>();
}
