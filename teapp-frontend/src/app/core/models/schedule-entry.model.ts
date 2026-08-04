import { Activity } from './activity.model';

// El orden debe coincidir con el enum DayOfWeek de Java en el backend.
export type DayOfWeek =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY'
  | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY:    'Lunes',
  TUESDAY:   'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY:  'Jueves',
  FRIDAY:    'Viernes',
  SATURDAY:  'Sábado',
  SUNDAY:    'Domingo'
};

/** Abreviaturas para los selectores, donde no entra el nombre completo. */
export const DAY_SHORT_LABELS: Record<DayOfWeek, string> = {
  MONDAY:    'Lun',
  TUESDAY:   'Mar',
  WEDNESDAY: 'Mié',
  THURSDAY:  'Jue',
  FRIDAY:    'Vie',
  SATURDAY:  'Sáb',
  SUNDAY:    'Dom'
};

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
];

export type TimeSlot = 'MORNING' | 'AFTERNOON' | 'NIGHT';

export const TIME_SLOT_LABELS: Record<TimeSlot, string> = {
  MORNING:   'Mañana',
  AFTERNOON: 'Tarde',
  NIGHT:     'Noche'
};

export const TIME_SLOTS: TimeSlot[] = ['MORNING', 'AFTERNOON', 'NIGHT'];

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

export interface WeeklySchedule {
  childId: string;
  week: Record<DayOfWeek, Record<TimeSlot, ScheduleEntry[]>>;
}
