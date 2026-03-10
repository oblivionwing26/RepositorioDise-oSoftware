import { Component } from '@angular/core';
import { EspectaculosService } from '../espectaculos';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'espectaculos',
  imports: [CommonModule],
  templateUrl: './espectaculos.html',
  styleUrl: './espectaculos.css',
})
export class Espectaculos {
  escenarios: any = [];

  constructor(private espectaculosService: EspectaculosService, private router: Router) {}

  getEscenarios() {
    this.espectaculosService.getEscenarios().subscribe(
      (response) => {
        this.escenarios = response;
            },
      (error) => {
        console.error('Error al obtener los espectaculos:', error);
      }

    )
    }

    getEspectaculos(escenario: any) {
      this.espectaculosService.getEspectaculos(escenario.id).subscribe(
        (response) => {
          escenario.espectaculos = response;
        },
        (error) => {
          console.error('Error al obtener los espectaculos:', error);
        }
      );
    }

    // getNumeroDeEntradas(espectaculo: any) {
    //   this.espectaculosService.getNumeroDeEntradas(espectaculo.id).subscribe(
    //     (response) => {
    //       espectaculo.entradasDisponibles = response;
    //     },
    //     (error) => {
    //       console.error('Error al obtener las entradas:', error);
    //     }
    //   );
    // }

    getNumeroDeEntradas(espectaculo: any) {
      this.espectaculosService.getNumeroDeEntradasComoDto(espectaculo.id).subscribe(
        (response) => {
          espectaculo.entradas = response;
        },
        (error) => {
          console.error('Error al obtener las entradas:', error);
        }
      );
    }

    getEntradasLibres(espectaculo: any) {
      this.espectaculosService.getEntradasLibres(espectaculo.id).subscribe(
        (response) => {
          espectaculo.entradasDisponibles = response;
        },
        (error) => {
          console.error('Error al obtener las entradas:', error);
        }
      );
    }

    irAComprarEntradas() {
      this.router.navigate(['/comprar']);
    }

}
