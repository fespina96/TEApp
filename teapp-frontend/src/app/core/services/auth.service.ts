import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';
import { environment } from '../../../environments/environment';

/**
 * Servicio de autenticación. Gestiona el token JWT.
 * - Si "rememberMe" está activo, persiste en localStorage (sobrevive al cierre del navegador/app).
 * - Si no, usa sessionStorage (se borra al cerrar la pestaña/app).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'teapp_token';
  private readonly USER_KEY  = 'teapp_user';
  private readonly apiUrl    = `${environment.apiUrl}/auth`;

  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUserFromStorage());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest, rememberMe = false): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.storeSession(response, rememberMe))
    );
  }

  // El registro siempre persiste la sesión (rememberMe = true).
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.storeSession(response, true))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;
    return !this.isTokenExpired(token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY) ?? sessionStorage.getItem(this.TOKEN_KEY);
  }

  get currentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isTherapist(): boolean {
    return this.currentUserSubject.value?.role === 'THERAPIST';
  }

  updateLocalAvatar(avatarBase64: string | null): void {
    const user = this.currentUserSubject.value;
    if (!user) return;
    const updated: User = { ...user, avatarBase64: avatarBase64 ?? undefined };
    const storage = localStorage.getItem(this.TOKEN_KEY) ? localStorage : sessionStorage;
    storage.setItem(this.USER_KEY, JSON.stringify(updated));
    this.currentUserSubject.next(updated);
  }

  private storeSession(response: AuthResponse, rememberMe: boolean): void {
    const user: User = {
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      avatarBase64: response.avatarBase64,
      role: response.role ?? 'PARENT',
      inviteCode: response.inviteCode
    };
    const storage = rememberMe ? localStorage : sessionStorage;
    storage.setItem(this.TOKEN_KEY, response.token);
    storage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private loadUserFromStorage(): User | null {
    const stored =
      localStorage.getItem(this.USER_KEY) ?? sessionStorage.getItem(this.USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  // Decodifica el payload del JWT (base64url) y compara el campo exp con la hora actual.
  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}
