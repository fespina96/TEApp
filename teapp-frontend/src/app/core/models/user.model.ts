/**
 * Modelo de usuario (padre/tutor o terapeuta) autenticado.
 */
export interface User {
  id: string;
  email: string;
  fullName: string;
  avatarBase64?: string;
  avatarColor?: string;
  role: 'PARENT' | 'THERAPIST';
  inviteCode?: string;
  dateOfBirth?: string;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
  id: string;
  email: string;
  fullName: string;
  avatarBase64?: string;
  avatarColor?: string;
  role: 'PARENT' | 'THERAPIST';
  inviteCode?: string;
  dateOfBirth?: string;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  dateOfBirth: string;
  role?: 'PARENT' | 'THERAPIST';
}
