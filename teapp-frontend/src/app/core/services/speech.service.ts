import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SpeechService {
  private synth = window.speechSynthesis;
  readonly isSupported = 'speechSynthesis' in window;

  speak(texto: string): void {
    if (!this.isSupported) return;
    this.synth.cancel();
    const enunciado = new SpeechSynthesisUtterance(texto);
    enunciado.lang = 'es-AR';
    enunciado.rate = 0.85;
    enunciado.pitch = 1.1;
    this.synth.speak(enunciado);
  }

  stop(): void {
    if (this.isSupported) this.synth.cancel();
  }
}
