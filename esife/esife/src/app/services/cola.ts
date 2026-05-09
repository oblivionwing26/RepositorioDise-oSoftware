import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface TurnoColaResponse {
  idTurno?: number;
  idEspectaculo: number;
  emailUsuario: string;
  estado: 'ESPERANDO' | 'ACTIVO' | 'EXPIRADO' | 'FINALIZADO';
  posicion: number;
  creadoEn?: string;
  activoHasta?: string;
  puedePrerreservar: boolean;
  colaActiva: boolean;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class ColaService {
  private readonly COLA_API = 'http://localhost:8080/cola';

  constructor(private http: HttpClient) {}

  entrar(idEspectaculo: number, tokenUsuario: string): Observable<TurnoColaResponse> {
    const params = new HttpParams()
      .set('idEspectaculo', idEspectaculo)
      .set('tokenUsuario', tokenUsuario);

    return this.http.post<TurnoColaResponse>(`${this.COLA_API}/entrar`, null, { params });
  }

  estado(idTurno: number, tokenUsuario: string): Observable<TurnoColaResponse> {
    const params = new HttpParams().set('tokenUsuario', tokenUsuario);

    return this.http.get<TurnoColaResponse>(`${this.COLA_API}/estado/${idTurno}`, { params });
  }

  finalizar(idTurno: number, tokenUsuario: string): Observable<TurnoColaResponse> {
    const params = new HttpParams().set('tokenUsuario', tokenUsuario);

    return this.http.post<TurnoColaResponse>(`${this.COLA_API}/finalizar/${idTurno}`, null, { params });
  }
}