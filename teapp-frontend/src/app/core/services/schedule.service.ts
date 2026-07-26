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

@Injectable({ providedIn: 'root' })
export class ScheduleService {

  constructor(private http: HttpClient) {}

  private baseUrl(childId: string): string {
    return `${environment.apiUrl}/children/${childId}/schedule`;
  }

  getWeeklySchedule(childId: string, day?: DayOfWeek): Observable<WeeklySchedule> {
    let params = new HttpParams();
    if (day) params = params.set('day', day);
    return this.http.get<WeeklySchedule>(this.baseUrl(childId), { params });
  }

  addEntry(childId: string, request: ScheduleEntryRequest): Observable<ScheduleEntry> {
    return this.http.post<ScheduleEntry>(this.baseUrl(childId), request);
  }

  updateEntry(childId: string, entryId: string, request: Partial<ScheduleEntryRequest>): Observable<ScheduleEntry> {
    return this.http.put<ScheduleEntry>(`${this.baseUrl(childId)}/${entryId}`, request);
  }

  deleteEntry(childId: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl(childId)}/${entryId}`);
  }

  markCompleted(childId: string, entryId: string, date: string): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl(childId)}/${entryId}/completions`, { date });
  }

  unmarkCompleted(childId: string, entryId: string, date: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl(childId)}/${entryId}/completions`, { body: { date } });
  }

  resetCurrentWeek(childId: string): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/children/${childId}/completions/current-week`);
  }
}
