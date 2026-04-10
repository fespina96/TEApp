import { Injectable } from '@angular/core';

/**
 * Servicio de síntesis de voz (Text-To-Speech).
 * Utiliza la Web Speech API para leer actividades en voz alta en el modo participante.
 */
@Injectable({ providedIn: 'root' })
export class SpeechService {
  private synth = window.speechSynthesis;
  /** Indica si el navegador soporta síntesis de voz */
  readonly isSupported = 'speechSynthesis' in window;

  /**
   * Lee el texto en voz alta en español.
   * Cancela cualquier lectura anterior antes de comenzar.
   * @param texto texto a leer
   */
  speak(texto: string): void {
    if (!this.isSupported) return;
    this.synth.cancel();
    const enunciado = new SpeechSynthesisUtterance(texto);
    enunciado.lang = 'es-AR';
    enunciado.rate = 0.85;
    enunciado.pitch = 1.1;
    this.synth.speak(enunciado);
  }

  /** Detiene la lectura en curso */
  stop(): void {
    if (this.isSupported) this.synth.cancel();
  }
}
