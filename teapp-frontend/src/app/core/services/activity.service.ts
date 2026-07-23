import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Activity, ActivityCategory, ActivityRequest, ActivityStep, ActivityStepRequest } from '../models/activity.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly apiUrl = `${environment.apiUrl}/activities`;

  constructor(private http: HttpClient) {}

  getAvailable(category?: ActivityCategory): Observable<Activity[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    return this.http.get<Activity[]>(this.apiUrl, { params });
  }

  getPredefined(): Observable<Activity[]> {
    return this.http.get<Activity[]>(`${this.apiUrl}/predefined`);
  }

  create(request: ActivityRequest): Observable<Activity> {
    return this.http.post<Activity>(this.apiUrl, request);
  }

  update(activityId: string, request: ActivityRequest): Observable<Activity> {
    return this.http.put<Activity>(`${this.apiUrl}/${activityId}`, request);
  }

  delete(activityId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${activityId}`);
  }

  getSteps(activityId: string): Observable<ActivityStep[]> {
    return this.http.get<ActivityStep[]>(`${this.apiUrl}/${activityId}/steps`);
  }

  saveSteps(activityId: string, steps: ActivityStepRequest[]): Observable<ActivityStep[]> {
    return this.http.put<ActivityStep[]>(`${this.apiUrl}/${activityId}/steps`, steps);
  }
}
