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
    tokenUsuario: string,
    tokenPrerreserva?: string | null
  ): Observable<PrerreservaResponse> {
    let params = new HttpParams()
      .set('idEntrada', idEntrada)
      .set('tokenUsuario', tokenUsuario);

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
    tokenUsuario: string,
    tokenPrerreserva: string
  ): Observable<void> {
    const params = new HttpParams()
      .set('tokenUsuario', tokenUsuario)
      .set('tokenPrerreserva', tokenPrerreserva);

    return this.http.delete<void>(
      `${this.RESERVAS_API}/prerreservar/${idEntrada}`,
      { params }
    );
  }
}
