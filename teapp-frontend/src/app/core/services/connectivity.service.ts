import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ConnectivityService {

  private connectedSubject = new BehaviorSubject<boolean>(true);
  connected$ = this.connectedSubject.asObservable();
  private checking = false;

  constructor(private http: HttpClient) {}

  check(): void {
    if (this.checking) return;
    this.checking = true;

    this.http.get<{ status: string }>(`${environment.apiUrl}/health`).subscribe({
      next: () => {
        this.connectedSubject.next(true);
        this.checking = false;
      },
      error: (err) => {
        if (err.status === 0) {
          this.connectedSubject.next(false);
        }
        this.checking = false;
      }
    });
  }

  markDisconnected(): void {
    this.connectedSubject.next(false);
  }

  get isConnected(): boolean {
    return this.connectedSubject.value;
  }
}
