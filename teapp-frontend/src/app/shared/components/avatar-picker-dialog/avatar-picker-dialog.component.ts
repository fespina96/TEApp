import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface AvatarPickerData {
  name: string;
  currentAvatar?: string;
}

const CATALOG: { emoji: string; bg: string }[] = [
  { emoji: '🦋', bg: '#C9B8E8' },
  { emoji: '🌟', bg: '#FAF0BE' },
  { emoji: '🚀', bg: '#A8D8EA' },
  { emoji: '🐱', bg: '#F9D8C0' },
  { emoji: '🐶', bg: '#FAF0BE' },
  { emoji: '🦁', bg: '#F9D8C0' },
  { emoji: '🐸', bg: '#B8E0C8' },
  { emoji: '🦄', bg: '#C9B8E8' },
  { emoji: '🌈', bg: '#A8D8EA' },
  { emoji: '🌺', bg: '#F9D8C0' },
  { emoji: '🌻', bg: '#FAF0BE' },
  { emoji: '⭐', bg: '#FAF0BE' },
  { emoji: '🎨', bg: '#C9B8E8' },
  { emoji: '🎵', bg: '#A8E0DA' },
  { emoji: '⚽', bg: '#B8E0C8' },
  { emoji: '🧩', bg: '#A8D8EA' },
  { emoji: '🏊', bg: '#A8E0DA' },
  { emoji: '🌙', bg: '#C9B8E8' },
  { emoji: '☀️', bg: '#FAF0BE' },
  { emoji: '🐻', bg: '#F9D8C0' },
];

@Component({
  selector: 'app-avatar-picker-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatTabsModule, MatTooltipModule],
  template: `
    <h2 mat-dialog-title>Foto de {{ data.name }}</h2>

    <mat-dialog-content>
      <!-- Preview central -->
      <div class="preview-row">
        <div class="preview-circle">
          <img *ngIf="preview" [src]="preview" class="preview-img" alt="preview">
          <span *ngIf="!preview" class="preview-letter">{{ data.name.charAt(0).toUpperCase() }}</span>
        </div>
        <button *ngIf="preview" mat-icon-button color="warn" class="clear-btn"
                matTooltip="Quitar foto" (click)="clearAvatar()">
          <mat-icon>cancel</mat-icon>
        </button>
      </div>

      <mat-tab-group animationDuration="200ms">

        <!-- Tab 1: Subir foto -->
        <mat-tab label="Subir foto">
          <div class="tab-content upload-tab">
            <p class="tab-hint">Sube una foto desde tu dispositivo.</p>
            <label class="upload-label">
              <mat-icon>upload</mat-icon>
              Elegir imagen
              <input type="file" accept="image/*" (change)="onFileSelected($event)" hidden>
            </label>
          </div>
        </mat-tab>

        <!-- Tab 2: Catálogo -->
        <mat-tab label="Catálogo">
          <div class="tab-content catalog-tab">
            <p class="tab-hint">Elige un avatar para el perfil.</p>
            <div class="catalog-grid">
              <button *ngFor="let item of catalog" type="button"
                      class="catalog-item"
                      [class.selected]="preview === emojiDataUrl(item.emoji, item.bg)"
                      [style.background-color]="item.bg"
                      (click)="selectCatalog(item.emoji, item.bg)"
                      [matTooltip]="item.emoji">
                {{ item.emoji }}
              </button>
            </div>
          </div>
        </mat-tab>

      </mat-tab-group>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancelar</button>
      <button mat-flat-button color="primary" (click)="confirm()" [disabled]="preview === initialAvatar">
        Guardar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .preview-row {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin: 0 0 16px;
    }

    .preview-circle {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: var(--color-sky-blue);
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      border: 3px solid var(--color-border);
      font-size: 2rem;
      font-weight: 700;
      color: var(--color-text-primary);
    }

    .preview-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .tab-content {
      padding: 16px 4px 8px;
    }

    .tab-hint {
      font-size: 0.85rem;
      color: var(--color-text-secondary);
      margin: 0 0 12px;
    }

    .upload-label {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 10px 20px;
      border-radius: var(--radius-md);
      background: var(--color-sky-blue);
      color: var(--color-text-primary);
      font-weight: 600;
      cursor: pointer;
      transition: filter 0.2s;
      &:hover { filter: brightness(0.92); }
      mat-icon { font-size: 20px; height: 20px; width: 20px; }
    }

    .catalog-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 8px;
    }

    .catalog-item {
      width: 52px;
      height: 52px;
      border-radius: 50%;
      border: 3px solid transparent;
      font-size: 1.6rem;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: transform 0.15s, border-color 0.15s;

      &:hover { transform: scale(1.1); }
      &.selected { border-color: var(--color-teal); transform: scale(1.1); }
    }

    .clear-btn { flex-shrink: 0; }
  `]
})
export class AvatarPickerDialogComponent {
  catalog = CATALOG;
  preview: string | undefined;
  initialAvatar: string | undefined;

  constructor(
    private dialogRef: MatDialogRef<AvatarPickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AvatarPickerData
  ) {
    this.preview = data.currentAvatar ?? undefined;
    this.initialAvatar = this.preview;
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX = 256;
        const ratio = Math.min(MAX / img.width, MAX / img.height);
        canvas.width  = img.width  * ratio;
        canvas.height = img.height * ratio;
        canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height);
        this.preview = canvas.toDataURL('image/jpeg', 0.85);
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  selectCatalog(emoji: string, bg: string): void {
    this.preview = this.emojiDataUrl(emoji, bg);
  }

  emojiDataUrl(emoji: string, bg: string): string {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
      <circle cx="50" cy="50" r="50" fill="${bg}"/>
      <text x="50" y="68" font-size="52" text-anchor="middle">${emoji}</text>
    </svg>`;
    return `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`;
  }

  clearAvatar(): void {
    this.preview = undefined;
  }

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    this.dialogRef.close(this.preview ?? null);
  }
}
