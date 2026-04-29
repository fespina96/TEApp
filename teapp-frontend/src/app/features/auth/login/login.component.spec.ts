import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const mockAuthResponse = {
    token: 'mock.token', expiresIn: 86400000, id: 'uuid',
    email: 'padre@test.com', fullName: 'Juan', avatarBase64: null,
    role: 'PARENT', inviteCode: null
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'register']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule, RouterTestingModule, NoopAnimationsModule],
      providers: [{ provide: AuthService, useValue: authServiceSpy }]
    }).compileComponents();

    fixture   = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router    = TestBed.inject(Router);
    fixture.detectChanges();
  });

  // ─── loginForm validación ─────────────────────────────────────────────────

  it('loginForm: debe estar inválido cuando está vacío', () => {
    expect(component.loginForm.valid).toBeFalse();
  });

  it('loginForm: email inválido → error de formato', () => {
    component.loginForm.get('email')!.setValue('no-es-email');
    expect(component.loginForm.get('email')!.hasError('email')).toBeTrue();
  });

  it('loginForm: email y contraseña válidos → formulario válido', () => {
    component.loginForm.setValue({ email: 'padre@test.com', password: 'Pass1234' });
    expect(component.loginForm.valid).toBeTrue();
  });

  // ─── registerForm validación ──────────────────────────────────────────────

  it('registerForm: contraseña sin mayúscula → error de pattern', () => {
    component.registerForm.get('password')!.setValue('pass1234');
    expect(component.registerForm.get('password')!.hasError('pattern')).toBeTrue();
  });

  it('registerForm: contraseña sin número → error de pattern', () => {
    component.registerForm.get('password')!.setValue('Password');
    expect(component.registerForm.get('password')!.hasError('pattern')).toBeTrue();
  });

  it('registerForm: contraseña válida (1 mayúscula + 1 número) → sin error de pattern', () => {
    component.registerForm.get('password')!.setValue('Password1');
    expect(component.registerForm.get('password')!.hasError('pattern')).toBeFalse();
  });

  it('registerForm: contraseñas no coinciden → error passwordsMismatch', () => {
    component.registerForm.patchValue({
      fullName: 'Juan', email: 'j@test.com',
      password: 'Pass1234', confirmPassword: 'OtraPass1'
    });
    expect(component.registerForm.hasError('passwordsMismatch')).toBeTrue();
  });

  it('registerForm: contraseñas iguales → sin error passwordsMismatch', () => {
    component.registerForm.patchValue({
      fullName: 'Juan', email: 'j@test.com',
      password: 'Pass1234', confirmPassword: 'Pass1234'
    });
    expect(component.registerForm.hasError('passwordsMismatch')).toBeFalse();
  });

  // ─── onLogin ──────────────────────────────────────────────────────────────

  it('onLogin: formulario inválido → no llama al servicio', () => {
    component.onLogin();
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('onLogin: éxito → navega al dashboard', () => {
    authServiceSpy.login.and.returnValue(of(mockAuthResponse as any));
    spyOn(router, 'navigate');

    component.loginForm.setValue({ email: 'padre@test.com', password: 'Pass1234', rememberMe: false });
    component.onLogin();

    expect(authServiceSpy.login).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/app/dashboard']);
  });

  it('onLogin: error 401 → muestra mensaje de credenciales incorrectas', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 401 })));

    component.loginForm.setValue({ email: 'padre@test.com', password: 'Wrong123', rememberMe: false });
    component.onLogin();

    expect(component.mensajeError).toBe('Email o contraseña incorrectos.');
    expect(component.cargando).toBeFalse();
  });

  it('onLogin: terapeuta → navega a /app/therapist', () => {
    const therapistResp = { ...mockAuthResponse, role: 'THERAPIST' };
    authServiceSpy.login.and.returnValue(of(therapistResp as any));
    spyOn(router, 'navigate');

    component.loginForm.setValue({ email: 'tera@test.com', password: 'Pass1234', rememberMe: false });
    component.onLogin();

    expect(router.navigate).toHaveBeenCalledWith(['/app/therapist']);
  });
});
