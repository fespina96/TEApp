/**
 * Modelo de perfil de niño administrado por un padre.
 */
export interface Child {
  id: string;
  name: string;
  dateOfBirth: string;
  avatarColor: string;
  avatarBase64?: string;
  notes?: string;
  createdAt: string;
}

export interface ChildRequest {
  name: string;
  dateOfBirth: string;
  avatarColor?: string;
  notes?: string;
}

/** Colores predefinidos para el avatar del niño */
export const AVATAR_COLORS = [
  { value: '#A8D8EA', label: 'Azul' },
  { value: '#B8E0C8', label: 'Verde' },
  { value: '#FAF0BE', label: 'Amarillo' },
  { value: '#C9B8E8', label: 'Lila' },
  { value: '#F9D8C0', label: 'Melocotón' },
  { value: '#A8E0DA', label: 'Turquesa' }
];
