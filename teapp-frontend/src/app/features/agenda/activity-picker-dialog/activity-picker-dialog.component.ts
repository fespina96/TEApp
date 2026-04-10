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
  // ── Catálogo ────────────────────────────────────────────────────────────────
  activities: Activity[] = [];
  filteredActivities: Activity[] = [];
  filteredPredefined: Activity[] = [];
  filteredTherapist: Activity[] = [];
  filteredCustom: Activity[] = [];
  loading = true;
  searchQuery = '';
  selectedCategory: ActivityCategory | null = null;
  addToAllDays = false;

  // ── Tabs ────────────────────────────────────────────────────────────────────
  activeTab: 'catalog' | 'new' = 'catalog';

  // ── Nueva actividad ─────────────────────────────────────────────────────────
  newForm!: FormGroup;
  saving = false;
  selectedIcon = 'star';
  selectedColor = '#F9D8C0';
  selectedImage: string | null = null;
  selectedPictogramUrl: string | null = null;

  // ── Temporizador de entrada ──────────────────────────────────────────────────
  entryDurationMinutes: number | null = null;
  entryPausable = true;
  entryRequireFullTimer = false;

  // ── ARASAAC ─────────────────────────────────────────────────────────────────
  imageMode: 'upload' | 'arasaac' = 'upload';
  arasaacQuery = '';
  arasaacResults: ArasaacPictogram[] = [];
  arasaacSearching = false;

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
      durationMinutes: [null],
      pausable:        [true]
    });

    this.newForm.get('category')!.valueChanges.subscribe((cat: ActivityCategory) => {
      this.selectedColor = CATEGORY_COLORS[cat] ?? '#F9D8C0';
    });

    this.activityService.getAvailable().subscribe({
      next: (activities) => {
        this.activities = activities;
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  // ── Catálogo ────────────────────────────────────────────────────────────────
  selectCategory(cat: ActivityCategory | null): void {
    this.selectedCategory = cat;
    this.applyFilter();
  }

  onSearch(): void { this.applyFilter(); }

  applyFilter(): void {
    const filtered = this.activities.filter(a => {
      const matchesCat    = !this.selectedCategory || a.category === this.selectedCategory;
      const matchesSearch = !this.searchQuery ||
                             a.name.toLowerCase().includes(this.searchQuery.toLowerCase());
      return matchesCat && matchesSearch;
    });
    this.filteredPredefined = filtered.filter(a => a.predefined);
    this.filteredTherapist  = filtered.filter(a => a.therapistCreated);
    this.filteredCustom     = filtered.filter(a => !a.predefined && !a.therapistCreated);
    this.filteredActivities = filtered;
  }

  selectActivity(activity: Activity): void {
    this.dialogRef.close({
      activity,
      addToAllDays: this.addToAllDays,
      durationMinutes: this.entryDurationMinutes || undefined,
      pausable: this.entryPausable,
      requireFullTimer: this.entryRequireFullTimer
    });
  }

  // ── Nueva actividad ─────────────────────────────────────────────────────────
  createAndSelect(): void {
    if (this.newForm.invalid) return;
    this.saving = true;

    const request: ActivityRequest = {
      name:            this.newForm.value.name.trim(),
      category:        this.newForm.value.category,
      iconName:        this.selectedIcon,
      color:           this.selectedColor,
      imageBase64:     this.selectedImage ?? undefined,
      pictogramUrl:    this.selectedPictogramUrl ?? undefined,
      durationMinutes: this.newForm.value.durationMinutes || undefined,
      pausable:        this.newForm.value.pausable ?? true
    };

    this.activityService.create(request).subscribe({
      next: (activity) => {
        this.dialogRef.close({
          activity,
          addToAllDays: this.addToAllDays,
          durationMinutes: this.entryDurationMinutes || undefined,
          pausable: this.entryPausable,
          requireFullTimer: this.entryRequireFullTimer
        });
      },
      error: () => { this.saving = false; }
    });
  }

  onImageSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.selectedImage = reader.result as string;
      this.selectedPictogramUrl = null;
    };
    reader.readAsDataURL(file);
  }

  removeImage(): void {
    this.selectedImage = null;
    this.selectedPictogramUrl = null;
  }

  // ── ARASAAC ─────────────────────────────────────────────────────────────────
  searchArasaac(): void {
    if (!this.arasaacQuery.trim()) return;
    this.arasaacSearching = true;
    this.arasaacResults = [];
    this.arasaacService.search(this.arasaacQuery.trim()).subscribe({
      next: (results) => {
        this.arasaacResults = results;
        this.arasaacSearching = false;
      },
      error: () => { this.arasaacSearching = false; }
    });
  }

  selectPictogram(pictogram: ArasaacPictogram): void {
    this.selectedPictogramUrl = this.arasaacService.imageUrl(pictogram._id);
    this.selectedImage = null;
  }

}
