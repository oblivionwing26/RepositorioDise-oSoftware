import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class Pagos {
  constructor(private http: HttpClient) {}

  prepararPago(pagoData: any) {
    return this.http.post('http://localhost:8080/pagos/prepararPago', pagoData, { responseType: 'text' });
  }

  confirmarPago(paymentIntentId: string) {
    return this.http.post('http://localhost:8080/pagos/confirmarPago', { paymentIntentId }, { responseType: 'text' });
  }
}
