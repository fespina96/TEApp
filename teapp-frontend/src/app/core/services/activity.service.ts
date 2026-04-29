import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Activity, ActivityCategory, ActivityRequest, ActivityStep, ActivityStepRequest } from '../models/activity.model';
import { environment } from '../../../environments/environment';

/**
 * Servicio de gestión del catálogo de actividades.
 */
@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly apiUrl = `${environment.apiUrl}/activities`;

  constructor(private http: HttpClient) {}

  /**
   * Retorna actividades disponibles (predefinidas + personalizadas del padre).
   * @param category filtro opcional por categoría
   */
  getAvailable(category?: ActivityCategory): Observable<Activity[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    return this.http.get<Activity[]>(this.apiUrl, { params });
  }

  /**
   * Retorna únicamente las actividades predefinidas del sistema.
   */
  getPredefined(): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.apiUrl}/predefined`);
  }

  /**
   * Crea una actividad personalizada.
   * @param request datos de la nueva actividad
   */
  create(request: ActivityRequest): Observable<Activity> {
    return this.http.post<Activity>(this.apiUrl, request);
  }

  /**
   * Actualiza una actividad personalizada existente.
   * @param activityId identificador de la actividad
   * @param request nuevos datos
   */
  update(activityId: string, request: ActivityRequest): Observable<Activity> {
    return this.http.put<Activity>(`${this.apiUrl}/${activityId}`, request);
  }

  /**
   * Elimina una actividad personalizada.
   * @param activityId identificador de la actividad
   */
  delete(activityId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${activityId}`);
  }

  /**
   * Retorna los pasos de una actividad.
   * @param activityId identificador de la actividad
   */
  getSteps(activityId: string): Observable<ActivityStep[]> {
    return this.http.get<ActivityStep[]>(`${this.apiUrl}/${activityId}/steps`);
  }

  /**
   * Guarda (reemplaza) los pasos de una actividad.
   * @param activityId identificador de la actividad
   * @param steps lista de pasos en el nuevo orden
   */
  saveSteps(activityId: string, steps: ActivityStepRequest[]): Observable<ActivityStep[]> {
    return this.http.put<ActivityStep[]>(`${this.apiUrl}/${activityId}/steps`, steps);
  }
}
