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

  // Genera un JWT con un payload decodificable y una expiración desplazada en segundos.
  const tokenConExpiracion = (segundosDesdeAhora: number): string => {
    const exp = Math.floor(Date.now() / 1000) + segundosDesdeAhora;
    return `eyJhbGciOiJIUzI1NiJ9.${btoa(JSON.stringify({ exp }))}.firma`;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  // login

  it('login: con "recordar dispositivo" almacena el token en localStorage', () => {
    service.login({ email: 'padre@test.com', password: 'Pass1234' }, true).subscribe(resp => {
      expect(resp.token).toBe('mock.jwt.token');
      expect(localStorage.getItem('teapp_token')).toBe('mock.jwt.token');
      expect(sessionStorage.getItem('teapp_token')).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'padre@test.com', password: 'Pass1234' });
    req.flush(mockAuthResponse);
  });

  it('login: sin "recordar dispositivo" almacena el token en sessionStorage', () => {
    service.login({ email: 'padre@test.com', password: 'Pass1234' }).subscribe(() => {
      expect(sessionStorage.getItem('teapp_token')).toBe('mock.jwt.token');
      expect(localStorage.getItem('teapp_token')).toBeNull();
    });

    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush(mockAuthResponse);
  });

  it('login: debe actualizar currentUser$ con los datos del usuario', () => {
    service.login({ email: 'padre@test.com', password: 'Pass1234' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush(mockAuthResponse);

    service.currentUser$.subscribe(user => {
      expect(user?.email).toBe('padre@test.com');
      expect(user?.role).toBe('PARENT');
    });
  });

  // logout

  it('logout: debe limpiar ambos storages y emitir null en currentUser$', () => {
    localStorage.setItem('teapp_token', 'some.token');
    sessionStorage.setItem('teapp_token', 'some.token');

    service.logout();

    expect(localStorage.getItem('teapp_token')).toBeNull();
    expect(sessionStorage.getItem('teapp_token')).toBeNull();
    service.currentUser$.subscribe(user => {
      expect(user).toBeNull();
    });
  });

  // isAuthenticated

  it('isAuthenticated: sin token → false', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('isAuthenticated: con token vigente → true', () => {
    localStorage.setItem('teapp_token', tokenConExpiracion(3600));
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('isAuthenticated: con token expirado → false', () => {
    localStorage.setItem('teapp_token', tokenConExpiracion(-3600));
    expect(service.isAuthenticated()).toBeFalse();
  });

  // getToken

  it('getToken: prioriza el token de localStorage sobre el de sessionStorage', () => {
    localStorage.setItem('teapp_token', 'token.local');
    sessionStorage.setItem('teapp_token', 'token.session');
    expect(service.getToken()).toBe('token.local');
  });

  it('getToken: usa sessionStorage cuando no hay token en localStorage', () => {
    sessionStorage.setItem('teapp_token', 'token.session');
    expect(service.getToken()).toBe('token.session');
  });

  // forgotPassword

  it('forgotPassword: debe hacer POST con el email', () => {
    service.forgotPassword('padre@test.com').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'padre@test.com' });
    req.flush(null);
  });

  // register

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

  it('register: no inicia sesión, el usuario tiene que entrar con sus credenciales', () => {
    service.register({ email: 'tera@test.com', password: 'Pass1234', fullName: 'Dra. Pérez', dateOfBirth: '1985-06-20', role: 'THERAPIST' }).subscribe(() => {
      expect(localStorage.getItem('teapp_token')).toBeNull();
      expect(sessionStorage.getItem('teapp_token')).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
    });
    httpMock.expectOne(`${environment.apiUrl}/auth/register`).flush(mockAuthResponse);
  });
});
