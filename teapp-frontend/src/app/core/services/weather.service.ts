import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface WeatherInfo {
  temp: number;
  emoji: string;
  label: string;
}

@Injectable({ providedIn: 'root' })
export class WeatherService {

  constructor(private http: HttpClient) {}

  /** Obtiene el clima actual usando geolocalización + Open-Meteo (sin API key). */
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
              map(data => this.parseWeather(data)),
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

  private parseWeather(data: any): WeatherInfo {
    const temp = Math.round(data.current.temperature_2m);
    const code = data.current.weathercode as number;
    return { temp, ...this.codeToInfo(code) };
  }

  private codeToInfo(code: number): { emoji: string; label: string } {
    if (code === 0)          return { emoji: '☀️',  label: 'Despejado' };
    if (code <= 3)           return { emoji: '🌤️',  label: 'Algo de nubes' };
    if (code <= 48)          return { emoji: '🌫️',  label: 'Niebla' };
    if (code <= 55)          return { emoji: '🌦️',  label: 'Llovizna' };
    if (code <= 65)          return { emoji: '🌧️',  label: 'Lluvia' };
    if (code <= 77)          return { emoji: '❄️',  label: 'Nieve' };
    if (code <= 82)          return { emoji: '🌦️',  label: 'Chubascos' };
    return                          { emoji: '⛈️',  label: 'Tormenta' };
  }
}
