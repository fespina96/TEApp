import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockAuthResponse: AuthResponse = {
    token: 'mock.jwt.token',
    expiresIn: 86400000,
    id: 'user-uuid',
    email: 'padre@test.com',
    fullName: 'Juan García',
    role: 'PARENT'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  // ─── login ───────────────────────────────────────────────────────────────

  it('login: debe hacer POST y almacenar el token en localStorage', () => {
    service.login({ email: 'padre@test.com', password: 'Pass1234' }).subscribe(resp => {
      expect(resp.token).toBe('mock.jwt.token');
      expect(localStorage.getItem('teapp_token')).toBe('mock.jwt.token');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'padre@test.com', password: 'Pass1234' });
    req.flush(mockAuthResponse);
  });

  it('login: debe actualizar currentUser$ con los datos del usuario', () => {
    service.login({ email: 'padre@test.com', password: 'Pass1234' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush(mockAuthResponse);

    service.currentUser$.subscribe(user => {
      expect(user?.email).toBe('padre@test.com');
      expect(user?.role).toBe('PARENT');
    });
  });

  // ─── logout ───────────────────────────────────────────────────────────────

  it('logout: debe limpiar localStorage y emitir null en currentUser$', () => {
    localStorage.setItem('teapp_token', 'some.token');

    service.logout();

    expect(localStorage.getItem('teapp_token')).toBeNull();
    service.currentUser$.subscribe(user => {
      expect(user).toBeNull();
    });
  });

  // ─── isAuthenticated ─────────────────────────────────────────────────────

  it('isAuthenticated: sin token → false', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('isAuthenticated: con token en localStorage → true', () => {
    localStorage.setItem('teapp_token', 'some.jwt.token');
    expect(service.isAuthenticated()).toBeTrue();
  });

  // ─── forgotPassword ───────────────────────────────────────────────────────

  it('forgotPassword: debe hacer POST con el email', () => {
    service.forgotPassword('padre@test.com').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'padre@test.com' });
    req.flush(null);
  });

  // ─── register ─────────────────────────────────────────────────────────────

  it('register: debe hacer POST y almacenar sesión', () => {
    const therapistResp: AuthResponse = { ...mockAuthResponse, role: 'THERAPIST', inviteCode: 'ABCD1234' };

    service.register({ email: 'tera@test.com', password: 'Pass1234', fullName: 'Dra. Pérez', dateOfBirth: '1985-06-20', role: 'THERAPIST' }).subscribe(resp => {
      expect(resp.role).toBe('THERAPIST');
      expect(resp.inviteCode).toBe('ABCD1234');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush(therapistResp);
  });
});
