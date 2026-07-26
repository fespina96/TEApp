export type ActivityCategory =
  | 'HYGIENE'
  | 'MEAL'
  | 'EDUCATION'
  | 'PLAY'
  | 'THERAPY'
  | 'REST'
  | 'OUTDOOR'
  | 'CUSTOM'
  | 'SPECIAL_EVENT';

export const CATEGORY_LABELS: Record<ActivityCategory, string> = {
  HYGIENE:   'Higiene',
  MEAL:      'Comidas',
  EDUCATION: 'Educación',
  PLAY:      'Juego',
  THERAPY:   'Terapia',
  REST:      'Descanso',
  OUTDOOR:       'Aire libre',
  CUSTOM:        'Personalizada',
  SPECIAL_EVENT: 'Evento especial'
};

export const CATEGORY_COLORS: Record<ActivityCategory, string> = {
  HYGIENE:   '#A8D8EA',
  MEAL:      '#FAF0BE',
  EDUCATION: '#B8E0C8',
  PLAY:      '#C9B8E8',
  THERAPY:   '#D4E8C8',
  REST:      '#E0D8F0',
  OUTDOOR:       '#C8E8D0',
  CUSTOM:        '#F9D8C0',
  SPECIAL_EVENT: '#FFE082'
};

export interface Activity {
  id: string;
  name: string;
  description?: string;
  category: ActivityCategory;
  iconName?: string;
  color: string;
  predefined: boolean;
  therapistCreated: boolean;
  imageBase64?: string;
  pictogramUrl?: string;
  durationMinutes?: number;
  pausable?: boolean;
  stepCount?: number;
  createdAt: string;
}

export interface ActivityStep {
  id?: string;
  stepOrder: number;
  title: string;
  description?: string;
  imageBase64?: string;
  pictogramUrl?: string;
}

export interface ActivityStepRequest {
  title: string;
  description?: string;
  imageBase64?: string;
  pictogramUrl?: string;
}

export interface ActivityRequest {
  name: string;
  description?: string;
  category: ActivityCategory;
  iconName?: string;
  color?: string;
  imageBase64?: string;
  pictogramUrl?: string;
  durationMinutes?: number;
  pausable?: boolean;
}
