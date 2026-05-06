import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CompraResponse {
  idEntrada: number;
  precio: number;
  estado: string;
  emailUsuario: string;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class ComprasService {
  private readonly COMPRAS_API = 'http://localhost:8080/compras';

  constructor(private http: HttpClient) {}

  comprar(tokenEntrada: string, tokenUsuario: string): Observable<CompraResponse> {
    const params = new HttpParams()
      .set('tokenEntrada', tokenEntrada)
      .set('tokenUsuario', tokenUsuario);

    return this.http.put<CompraResponse>(`${this.COMPRAS_API}/comprar`, null, {
      params,
    });
  }
}
