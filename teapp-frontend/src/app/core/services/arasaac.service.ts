import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ArasaacPictogram {
  _id: number;
  keyword: string;
}

const ARASAAC_API  = 'https://api.arasaac.org/v1/pictograms';
const ARASAAC_STATIC = 'https://static.arasaac.org/pictograms';

@Injectable({ providedIn: 'root' })
export class ArasaacService {

  constructor(private http: HttpClient) {}

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

  // El CDN estático es más fiable dentro de <img> que el endpoint de la API.
  thumbUrl(id: number): string {
    return `${ARASAAC_STATIC}/${id}/${id}_300.png`;
  }

  imageUrl(id: number): string {
    return `${ARASAAC_STATIC}/${id}/${id}_500.png`;
  }
}
