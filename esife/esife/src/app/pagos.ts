import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PagoPreparadoResponse {
  idPago: number;
  centimos: number;
  moneda: string;
  metodo: string;
  estado: string;
  tokenPago: string;
  clientSecret: string;
  publicKey: string;
}

@Injectable({
  providedIn: 'root',
})
export class Pagos {
  constructor(private http: HttpClient) {}

  prepararPago(pagoData: any): Observable<PagoPreparadoResponse> {
    return this.http.post<PagoPreparadoResponse>('http://localhost:8080/pagos/prepararPago', pagoData);
  }

  confirmarPago(paymentIntentId: string) {
    return this.http.post('http://localhost:8080/pagos/confirmarPago', { paymentIntentId }, { responseType: 'text' });
  }
}
