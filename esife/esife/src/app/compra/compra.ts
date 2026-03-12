import { Component } from '@angular/core';
import { Pagos } from '../pagos';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-compra',
  imports: [CommonModule, FormsModule],
  templateUrl: './compra.html',
  styleUrl: './compra.css',
})
export class Compra {

  clientSecret?: string = '';
  importe: number = 0;

  constructor(private pagosService: Pagos) {}

  irAlPago() {
    let jsonData = {
      centimos: Math.floor(this.importe.valueOf() * 100)
    };
    this.pagosService.prepararPago(jsonData).subscribe(
      response => {
        this.clientSecret = response;
      },
      error => {
        console.error('Error al preparar el pago:', error);
      }
    );
  }
}
