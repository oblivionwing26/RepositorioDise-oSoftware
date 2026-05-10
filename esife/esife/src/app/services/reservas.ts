import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PrerreservaResponse {
  idEntrada: number;
  tokenEntrada: string;
  expiraEn: string;
  precio: number; // idealmente precio total de la prerreserva
}

@Injectable({ providedIn: 'root' })
export class ReservasService {
  private readonly RESERVAS_API = 'http://localhost:8080/reservas';

  constructor(private http: HttpClient) {}

  prerreservar(
    idEntrada: number,
    tokenUsuario?: string | null,
    clienteId?: string | null,
    idTurno?: number | null,
    tokenPrerreserva?: string | null
  ): Observable<PrerreservaResponse> {
    let params = this.paramsIdentidad(tokenUsuario, clienteId)
      .set('idEntrada', idEntrada);

    if (idTurno != null) {
      params = params.set('idTurno', idTurno);
    }

    if (tokenPrerreserva) {
      params = params.set('tokenPrerreserva', tokenPrerreserva);
    }

    return this.http.put<PrerreservaResponse>(
      `${this.RESERVAS_API}/prerreservar`,
      null,
      { params }
    );
  }

  liberarEntradaPrerreservada(
    idEntrada: number,
    tokenUsuario: string | null,
    clienteId: string | null,
    tokenPrerreserva: string,
  ): Observable<void> {
    const params = this.paramsIdentidad(tokenUsuario, clienteId)
      .set('tokenPrerreserva', tokenPrerreserva);

    return this.http.delete<void>(
      `${this.RESERVAS_API}/prerreservar/${idEntrada}`,
      { params }
    );
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
