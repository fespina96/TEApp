import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ForgotPasswordComponent } from './forgot-password.component';
import { AuthService } from '../../../core/services/auth.service';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['forgotPassword']);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, ReactiveFormsModule, RouterTestingModule, NoopAnimationsModule],
      providers: [{ provide: AuthService, useValue: authServiceSpy }]
    }).compileComponents();

    fixture   = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('enviar: formulario inválido → no llama al servicio', () => {
    component.enviar();
    expect(authServiceSpy.forgotPassword).not.toHaveBeenCalled();
  });

  it('enviar: éxito → marca enviado y deja de cargar', () => {
    authServiceSpy.forgotPassword.and.returnValue(of(void 0));
    component.form.setValue({ email: 'padre@test.com' });

    component.enviar();

    expect(component.enviado).toBeTrue();
    expect(component.cargando).toBeFalse();
    expect(component.mensajeError).toBe('');
  });

  it('enviar: error de red (status 0) → muestra mensaje y no marca enviado', () => {
    authServiceSpy.forgotPassword.and.returnValue(throwError(() => ({ status: 0 })));
    component.form.setValue({ email: 'padre@test.com' });

    component.enviar();

    expect(component.enviado).toBeFalse();
    expect(component.mensajeError).toContain('No se pudo conectar');
    expect(component.cargando).toBeFalse();
  });

  it('enviar: error HTTP (no de red) → marca enviado por seguridad', () => {
    // No debe revelar si el email existe: cualquier respuesta del servidor muestra éxito.
    authServiceSpy.forgotPassword.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.setValue({ email: 'inexistente@test.com' });

    component.enviar();

    expect(component.enviado).toBeTrue();
    expect(component.mensajeError).toBe('');
  });
});
