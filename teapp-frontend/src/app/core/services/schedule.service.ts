import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DayOfWeek,
  ScheduleEntry,
  ScheduleEntryRequest,
  WeeklySchedule
} from '../models/schedule-entry.model';
import { environment } from '../../../environments/environment';

/**
 * Servicio de gestión de la agenda semanal de los niños.
 */
@Injectable({ providedIn: 'root' })
export class ScheduleService {

  constructor(private http: HttpClient) {}

  private baseUrl(childId: string): string {
    return `${environment.apiUrl}/children/${childId}/schedule`;
  }

  /**
   * Obtiene la agenda semanal completa de un niño.
   * @param childId  identificador del niño
   * @param day      filtro opcional por día de la semana
   */
  getWeeklySchedule(childId: string, day?: DayOfWeek): Observable<WeeklySchedule> {
    let params = new HttpParams();
    if (day) params = params.set('day', day);
    return this.http.get<WeeklySchedule>(this.baseUrl(childId), { params });
  }

  /**
   * Agrega una actividad a la agenda del niño.
   * @param childId  identificador del niño
   * @param request  datos de la entrada
   */
  addEntry(childId: string, request: ScheduleEntryRequest): Observable<ScheduleEntry> {
    return this.http.post<ScheduleEntry>(this.baseUrl(childId), request);
  }

  /**
   * Actualiza una entrada existente de la agenda.
   * @param childId  identificador del niño
   * @param entryId  identificador de la entrada
   * @param request  datos actualizados
   */
  updateEntry(childId: string, entryId: string, request: Partial<ScheduleEntryRequest>): Observable<ScheduleEntry> {
    return this.http.put<ScheduleEntry>(`${this.baseUrl(childId)}/${entryId}`, request);
  }

  /**
   * Elimina una entrada de la agenda.
   * @param childId  identificador del niño
   * @param entryId  identificador de la entrada
   */
  deleteEntry(childId: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl(childId)}/${entryId}`);
  }

  /**
   * Marca una entrada de agenda como completada en una fecha dada.
   * @param childId  identificador del niño
   * @param entryId  identificador de la entrada
   * @param date     fecha en formato ISO (YYYY-MM-DD)
   */
  markCompleted(childId: string, entryId: string, date: string): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl(childId)}/${entryId}/completions`, { date });
  }

  /**
   * Desmarca una entrada de agenda como completada en una fecha dada.
   * @param childId  identificador del niño
   * @param entryId  identificador de la entrada
   * @param date     fecha en formato ISO (YYYY-MM-DD)
   */
  unmarkCompleted(childId: string, entryId: string, date: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl(childId)}/${entryId}/completions`, { body: { date } });
  }

  /**
   * Elimina todas las completitudes de la semana actual del participante.
   * @param childId identificador del niño
   */
  resetCurrentWeek(childId: string): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/children/${childId}/completions/current-week`);
  }
}
