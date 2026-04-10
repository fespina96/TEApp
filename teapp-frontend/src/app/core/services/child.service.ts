import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Child, ChildRequest } from '../models/child.model';
import { environment } from '../../../environments/environment';

/**
 * Servicio de gestión de perfiles de niños.
 * Todas las peticiones incluyen el JWT automáticamente via el interceptor.
 */
@Injectable({ providedIn: 'root' })
export class ChildService {
  private readonly apiUrl = `${environment.apiUrl}/children`;

  constructor(private http: HttpClient) {}

  /**
   * Retorna todos los hijos del padre autenticado.
   */
  getAll(): Observable<Child[]> {
    return this.http.get<Child[]>(this.apiUrl);
  }

  /**
   * Retorna el perfil de un hijo específico.
   * @param childId identificador del niño
   */
  getById(childId: string): Observable<Child> {
    return this.http.get<Child>(`${this.apiUrl}/${childId}`);
  }

  /**
   * Crea un nuevo perfil de niño.
   * @param request datos del niño
   */
  create(request: ChildRequest): Observable<Child> {
    return this.http.post<Child>(this.apiUrl, request);
  }

  /**
   * Actualiza el perfil de un niño existente.
   * @param childId identificador del niño
   * @param request nuevos datos
   */
  update(childId: string, request: ChildRequest): Observable<Child> {
    return this.http.put<Child>(`${this.apiUrl}/${childId}`, request);
  }

  /**
   * Actualiza el avatar (base64 o data URL) de un participante.
   */
  updateAvatar(childId: string, avatarBase64: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${childId}/avatar`, avatarBase64, {
      headers: { 'Content-Type': 'text/plain' }
    });
  }

  /**
   * Elimina el perfil de un niño y su agenda en cascada.
   * @param childId identificador del niño
   */
  delete(childId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${childId}`);
  }
}
