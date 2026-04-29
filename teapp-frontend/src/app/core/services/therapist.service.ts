import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SupervisedParent {
  id: string;
  fullName: string;
  email: string;
  avatarBase64?: string;
}

export interface LinkedTherapist {
  id: string;
  fullName: string;
  email: string;
  avatarBase64?: string;
}

/**
 * Servicio de vinculación y supervisión entre padres y terapeutas.
 * Los padres usan este servicio para vincularse a un terapeuta mediante código de invitación.
 * Los terapeutas lo usan para acceder a las agendas de los participantes de sus padres.
 */
@Injectable({ providedIn: 'root' })
export class TherapistService {
  private readonly apiUrl = `${environment.apiUrl}/therapist`;

  constructor(private http: HttpClient) {}

  /**
   * Vincula al padre autenticado con un terapeuta usando su código de invitación.
   * @param inviteCode código de invitación del terapeuta
   */
  linkToTherapist(inviteCode: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/link`, { inviteCode });
  }

  /**
   * Desvincula al padre autenticado de un terapeuta.
   * @param therapistId identificador del terapeuta a desvincular
   */
  unlinkFromTherapist(therapistId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/link/${therapistId}`);
  }

  /**
   * Retorna la lista de padres supervisados por el terapeuta autenticado.
   */
  getSupervisedParents(): Observable<SupervisedParent[]> {
    return this.http.get<SupervisedParent[]>(`${this.apiUrl}/supervised`);
  }

  /**
   * Retorna los terapeutas vinculados al padre autenticado.
   */
  getMyTherapists(): Observable<LinkedTherapist[]> {
    return this.http.get<LinkedTherapist[]>(`${this.apiUrl}/my-therapists`);
  }

  /**
   * Retorna los participantes de un padre supervisado.
   * @param parentId identificador del padre
   */
  getSupervisedChildren(parentId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/supervised/${parentId}/children`);
  }

  /**
   * Retorna la agenda semanal de un participante de un padre supervisado.
   * @param parentId identificador del padre
   * @param childId identificador del participante
   */
  getSupervisedSchedule(parentId: string, childId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/supervised/${parentId}/children/${childId}/schedule`);
  }
}
