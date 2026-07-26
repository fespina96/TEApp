import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SupervisedParent {
  id: string;
  fullName: string;
  email: string;
  avatarBase64?: string;
}

export interface LinkedTherapist {
  id: string;
  fullName: string;
  email: string;
  avatarBase64?: string;
}

@Injectable({ providedIn: 'root' })
export class TherapistService {
  private readonly apiUrl = `${environment.apiUrl}/therapist`;

  constructor(private http: HttpClient) {}

  linkToTherapist(inviteCode: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/link`, { inviteCode });
  }

  unlinkFromTherapist(therapistId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/link/${therapistId}`);
  }

  getSupervisedParents(): Observable<SupervisedParent[]> {
    return this.http.get<SupervisedParent[]>(`${this.apiUrl}/supervised`);
  }

  getMyTherapists(): Observable<LinkedTherapist[]> {
    return this.http.get<LinkedTherapist[]>(`${this.apiUrl}/my-therapists`);
  }

  getSupervisedChildren(parentId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/supervised/${parentId}/children`);
  }

  getSupervisedSchedule(parentId: string, childId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/supervised/${parentId}/children/${childId}/schedule`);
  }
}
