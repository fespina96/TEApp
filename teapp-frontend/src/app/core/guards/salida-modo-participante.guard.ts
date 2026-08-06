import { CanDeactivateFn } from '@angular/router';
import { Observable } from 'rxjs';

/** Lo implementa la pantalla que puede estar en modo participante. */
export interface PuedeSalirDelModoParticipante {
  puedeSalir(): boolean | Observable<boolean>;
}

/**
 * Impide abandonar la agenda mientras el modo participante está activo sin
 * verificar la contraseña del adulto.
 *
 * La comprobación va en la navegación y no en el botón "Salir", porque el botón
 * atrás del navegador es otra forma de irse y tiene que pedir lo mismo.
 */
export const salidaModoParticipanteGuard: CanDeactivateFn<PuedeSalirDelModoParticipante> =
  (componente) => componente.puedeSalir();
