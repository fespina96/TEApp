import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

/**
 * Servicio de gestión del perfil del usuario autenticado.
 * Permite actualizar el avatar y cambiar la contraseña.
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  /**
   * Actualiza la foto de perfil del usuario autenticado.
   * @param avatarBase64 imagen codificada en base64 o cadena vacía para eliminarla
   */
  updateAvatar(avatarBase64: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/avatar`, avatarBase64, {
      headers: { 'Content-Type': 'text/plain' }
    });
  }

  /**
   * Cambia la contraseña del usuario autenticado.
   * @param currentPassword contraseña actual para verificación
   * @param newPassword nueva contraseña a establecer
   */
  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, { currentPassword, newPassword });
  }

  /** Retorna el perfil completo del usuario autenticado (incluye createdAt). */
  getMe(): Observable<AuthResponse> {
    return this.http.get<AuthResponse>(`${this.apiUrl}/me`);
  }

  /** Actualiza nombre completo y fecha de nacimiento del usuario. */
  updateProfile(fullName: string, dateOfBirth?: string): Observable<AuthResponse> {
    return this.http.put<AuthResponse>(`${this.apiUrl}/me`, { fullName, dateOfBirth });
  }

  /** Elimina lógicamente la cuenta del usuario autenticado. */
  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }
}
