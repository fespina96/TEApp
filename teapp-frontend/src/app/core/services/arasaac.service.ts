import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ArasaacPictogram {
  _id: number;
  keyword: string;
}

const ARASAAC_API  = 'https://api.arasaac.org/v1/pictograms';
const ARASAAC_STATIC = 'https://static.arasaac.org/pictograms';

/**
 * Servicio de búsqueda de pictogramas en la API pública de ARASAAC.
 * No requiere autenticación. Usa el CDN estático de ARASAAC para las imágenes.
 */
@Injectable({ providedIn: 'root' })
export class ArasaacService {

  constructor(private http: HttpClient) {}

  /** Busca pictogramas por palabra clave (API pública, sin auth) */
  search(term: string): Observable<ArasaacPictogram[]> {
    return this.http
      .get<any[]>(`${ARASAAC_API}/es/search/${encodeURIComponent(term)}`)
      .pipe(
        map(results =>
          results.slice(0, 24).map(r => ({
            _id: r._id,
            keyword: r.keywords?.[0]?.keyword ?? ''
          }))
        )
      );
  }

  /** URL de miniatura estática (CDN de ARASAAC, más fiable en <img>) */
  thumbUrl(id: number): string {
    return `${ARASAAC_STATIC}/${id}/${id}_300.png`;
  }

  /** URL de imagen completa para guardar en la actividad */
  imageUrl(id: number): string {
    return `${ARASAAC_STATIC}/${id}/${id}_500.png`;
  }
}
