import { Activity } from './activity.model';

/**
 * Días de la semana en el orden utilizado por el backend (Java DayOfWeek).
 */
export type DayOfWeek =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY'
  | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** Etiquetas en español para cada día */
export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY:    'Lunes',
  TUESDAY:   'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY:  'Jueves',
  FRIDAY:    'Viernes',
  SATURDAY:  'Sábado',
  SUNDAY:    'Domingo'
};

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
];

/**
 * Franjas horarias del día.
 */
export type TimeSlot = 'MORNING' | 'AFTERNOON' | 'NIGHT';

/** Etiquetas en español para cada franja */
export const TIME_SLOT_LABELS: Record<TimeSlot, string> = {
  MORNING:   'Mañana',
  AFTERNOON: 'Tarde',
  NIGHT:     'Noche'
};

export const TIME_SLOTS: TimeSlot[] = ['MORNING', 'AFTERNOON', 'NIGHT'];

/**
 * Modelo de entrada en la agenda semanal.
 */
export interface ScheduleEntry {
  id: string;
  activity: Activity;
  dayOfWeek: DayOfWeek;
  timeSlot: TimeSlot;
  startTime?: string;
  endTime?: string;
  sortOrder: number;
  notes?: string;
  durationMinutes?: number;
  pausable?: boolean;
  requireFullTimer?: boolean;
  completedDates: string[];
}

export interface ScheduleEntryRequest {
  activityId: string;
  dayOfWeek: DayOfWeek;
  timeSlot: TimeSlot;
  startTime?: string;
  endTime?: string;
  sortOrder?: number;
  notes?: string;
  durationMinutes?: number;
  pausable?: boolean;
  requireFullTimer?: boolean;
}

/**
 * Respuesta de agenda semanal completa del backend.
 */
export interface WeeklySchedule {
  childId: string;
  week: Record<DayOfWeek, Record<TimeSlot, ScheduleEntry[]>>;
}
