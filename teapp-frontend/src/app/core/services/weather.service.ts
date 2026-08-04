import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ArasaacService } from './arasaac.service';

/**
 * El clima que ve el participante: una frase corta y un pictograma, sin números.
 * La temperatura en grados no le dice nada a quien todavía no lee cifras.
 */
export interface WeatherInfo {
  frase: string;
  pictogramaUrl: string;
}

/** Pictogramas de ARASAAC verificados contra su CDN. */
const PICTO = {
  sol:      2798,
  calor:    35561,
  frio:     4652,
  lluvia:   7148,
  nublado:  2882,
  nieve:    7172,
  tormenta: 34892,
  niebla:   35049
};

/** Debajo de esto se siente frío; por encima del otro, calor. */
const FRIO_HASTA   = 12;
const CALOR_DESDE  = 27;

@Injectable({ providedIn: 'root' })
export class WeatherService {

  constructor(private http: HttpClient, private arasaac: ArasaacService) {}

  // Geolocaliza al usuario y consulta Open-Meteo (API abierta, sin key). Devuelve null si algo falla.
  getWeather(): Observable<WeatherInfo | null> {
    return new Observable(observer => {
      if (!navigator.geolocation) {
        observer.next(null);
        observer.complete();
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude.toFixed(4);
          const lon = pos.coords.longitude.toFixed(4);
          this.http
            .get<any>(
              `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,weathercode&timezone=auto`
            )
            .pipe(
              map(data => this.interpretar(data)),
              catchError(() => of(null))
            )
            .subscribe(result => {
              observer.next(result);
              observer.complete();
            });
        },
        () => {
          observer.next(null);
          observer.complete();
        },
        { timeout: 6000 }
      );
    });
  }

  private interpretar(data: any): WeatherInfo {
    const temp = Math.round(data.current.temperature_2m);
    const code = data.current.weathercode as number;
    const { frase, picto } = this.describir(code, temp);
    return { frase, pictogramaUrl: this.arasaac.imageUrl(picto) };
  }

  /**
   * Lo que llueve o nieva manda sobre la temperatura: si está lloviendo, eso es
   * lo que hay que abrigar o llevar, no si hace uno o dos grados de más.
   */
  private describir(code: number, temp: number): { frase: string; picto: number } {
    if (code >= 95)                  return { frase: 'Afuera hay tormenta',   picto: PICTO.tormenta };
    if (code >= 71 && code <= 77)    return { frase: 'Afuera está nevando',   picto: PICTO.nieve };
    if (code >= 51 && code <= 82)    return { frase: 'Afuera está lloviendo', picto: PICTO.lluvia };
    if (code >= 45 && code <= 48)    return { frase: 'Afuera hay niebla',     picto: PICTO.niebla };

    if (temp <= FRIO_HASTA)          return { frase: 'Afuera hace frío',      picto: PICTO.frio };
    if (temp >= CALOR_DESDE)         return { frase: 'Afuera hace calor',     picto: PICTO.calor };

    return code === 0
      ? { frase: 'Afuera está soleado', picto: PICTO.sol }
      : { frase: 'Afuera está nublado', picto: PICTO.nublado };
  }
}
