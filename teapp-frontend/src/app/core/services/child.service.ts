import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Child, ChildRequest } from '../models/child.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChildService {
  private readonly apiUrl = `${environment.apiUrl}/children`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Child[]> {
    return this.http.get<Child[]>(this.apiUrl);
  }

  getById(childId: string): Observable<Child> {
    return this.http.get<Child>(`${this.apiUrl}/${childId}`);
  }

  create(request: ChildRequest): Observable<Child> {
    return this.http.post<Child>(this.apiUrl, request);
  }

  update(childId: string, request: ChildRequest): Observable<Child> {
    return this.http.put<Child>(`${this.apiUrl}/${childId}`, request);
  }

  updateAvatar(childId: string, avatarBase64: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${childId}/avatar`, avatarBase64, {
      headers: { 'Content-Type': 'text/plain' }
    });
  }

  delete(childId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${childId}`);
  }
}
