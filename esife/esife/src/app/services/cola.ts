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

  entrar(
    idEspectaculo: number,
    tokenUsuario?: string | null,
    clienteId?: string | null,
  ): Observable<TurnoColaResponse> {
    let params = this.paramsIdentidad(tokenUsuario, clienteId)
      .set('idEspectaculo', idEspectaculo);

    return this.http.post<TurnoColaResponse>(`${this.COLA_API}/entrar`, null, { params });
  }

  estado(
    idTurno: number,
    tokenUsuario?: string | null,
    clienteId?: string | null,
  ): Observable<TurnoColaResponse> {
    const params = this.paramsIdentidad(tokenUsuario, clienteId);

    return this.http.get<TurnoColaResponse>(`${this.COLA_API}/estado/${idTurno}`, { params });
  }

  finalizar(
    idTurno: number,
    tokenUsuario?: string | null,
    clienteId?: string | null,
  ): Observable<TurnoColaResponse> {
    const params = this.paramsIdentidad(tokenUsuario, clienteId);

    return this.http.post<TurnoColaResponse>(`${this.COLA_API}/finalizar/${idTurno}`, null, { params });
  }

  private paramsIdentidad(tokenUsuario?: string | null, clienteId?: string | null): HttpParams {
    let params = new HttpParams();

    if (tokenUsuario) {
      params = params.set('tokenUsuario', tokenUsuario);
    } else if (clienteId) {
      params = params.set('clienteId', clienteId);
    }

    return params;
  }
}