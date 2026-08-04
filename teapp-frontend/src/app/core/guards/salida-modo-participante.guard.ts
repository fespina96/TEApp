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
 * El botón "Salir" ya la pedía, pero el botón atrás del navegador se saltaba esa
 * verificación y el modo dejaba de ser una barrera: cualquiera podía volver a la
 * vista del adulto con un gesto. La comprobación tiene que estar en la
 * navegación, no en un botón.
 */
export const salidaModoParticipanteGuard: CanDeactivateFn<PuedeSalirDelModoParticipante> =
  (componente) => componente.puedeSalir();
