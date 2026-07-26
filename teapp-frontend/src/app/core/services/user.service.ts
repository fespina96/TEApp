import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  updateAvatar(avatarBase64: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/avatar`, avatarBase64, {
      headers: { 'Content-Type': 'text/plain' }
    });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, { currentPassword, newPassword });
  }

  getMe(): Observable<AuthResponse> {
    return this.http.get<AuthResponse>(`${this.apiUrl}/me`);
  }

  updateProfile(fullName: string, dateOfBirth?: string): Observable<AuthResponse> {
    return this.http.put<AuthResponse>(`${this.apiUrl}/me`, { fullName, dateOfBirth });
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }
}
