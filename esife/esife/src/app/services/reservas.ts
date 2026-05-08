import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PrerreservaResponse {
  idEntrada: number;
  tokenEntrada: string;
  expiraEn: string;
  precio: number;
}

@Injectable({ providedIn: 'root' })
export class ReservasService {
  private readonly RESERVAS_API = 'http://localhost:8080/reservas';

  constructor(private http: HttpClient) {}

  prerreservar(idEntrada: number, tokenUsuario: string, idTurno?: number): Observable<PrerreservaResponse> {
    const params = new HttpParams()
      .set('idEntrada', idEntrada)
      .set('tokenUsuario', tokenUsuario);

    const paramsConTurno = idTurno == null ? params : params.set('idTurno', idTurno);

    return this.http.put<PrerreservaResponse>(`${this.RESERVAS_API}/prerreservar`, null, { params: paramsConTurno });
  }
}
