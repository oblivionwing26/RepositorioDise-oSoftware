import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ComprasService {
  private readonly COMPRAS_API = 'http://localhost:8080/compras';

  constructor(private http: HttpClient) {}

  comprar(tokenEntrada: string, tokenUsuario: string): Observable<string> {
    const params = new HttpParams()
      .set('tokenEntrada', tokenEntrada)
      .set('tokenUsuario', tokenUsuario);

    return this.http.put(`${this.COMPRAS_API}/comprar`, null, {
      params,
      responseType: 'text',
    });
  }
}
