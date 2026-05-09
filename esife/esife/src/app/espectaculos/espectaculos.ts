import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Auth } from '../services/auth';
import {
  EntradaDisponible,
  EscenarioDto,
  EspectaculoDto,
  EspectaculosService,
} from '../espectaculos';

@Component({
  selector: 'espectaculos',
  imports: [CommonModule],
  templateUrl: './espectaculos.html',
  styleUrl: './espectaculos.css',
})
export class Espectaculos implements OnInit {
  private readonly COMPRA_STORAGE_KEY = 'compraPendiente';
  escenarios: EscenarioDto[] = [];
  error: string | null = null;
  loadingEscenarios = false;

  constructor(
    private espectaculosService: EspectaculosService,
    private auth: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.getEscenarios();
  }

  getEscenarios(): void {
    this.error = null;
    this.loadingEscenarios = true;

    this.espectaculosService.getEscenarios().subscribe({
      next: escenarios => {
        this.escenarios = escenarios;
        this.loadingEscenarios = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar los escenarios.';
        this.loadingEscenarios = false;
        this.cdr.detectChanges();
      },
    });
  }

  getEspectaculos(escenario: EscenarioDto): void {
    escenario.loading = true;
    this.error = null;

    this.espectaculosService.getEspectaculos(escenario.id).subscribe({
      next: espectaculos => {
        escenario.espectaculos = espectaculos;
        escenario.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar los espectaculos.';
        escenario.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getNumeroDeEntradas(espectaculo: EspectaculoDto): void {
    espectaculo.loadingEntradas = true;
    this.error = null;

    forkJoin({
      resumen: this.espectaculosService.getNumeroDeEntradasComoDto(espectaculo.id),
      disponibles: this.espectaculosService.getEntradasDisponibles(espectaculo.id),
    }).subscribe({
      next: resultado => {
        espectaculo.entradas = resultado.resumen;
        espectaculo.entradasDisponibles = resultado.disponibles;
        espectaculo.loadingEntradas = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la disponibilidad de entradas.';
        espectaculo.loadingEntradas = false;
        this.cdr.detectChanges();
      },
    });
  }

  seleccionarEntrada(espectaculo: EspectaculoDto, entrada: EntradaDisponible): void {
    this.guardarCompraPendiente(espectaculo, entrada);

    if (!this.auth.isLogged()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    this.router.navigate(['/comprar'], {
      state: { espectaculo, entrada },
    });
  }

  etiquetaEntrada(entrada: EntradaDisponible): string {
    if (entrada.tipo === 'ZONA') {
      return `Zona ${entrada.zona ?? '-'}`;
    }

    if (entrada.tipo === 'PRECISA') {
      return `Planta ${entrada.planta ?? '-'} · Fila ${entrada.fila ?? '-'} · Butaca ${entrada.columna ?? '-'}`;
    }

    return 'Entrada general';
  }

  private guardarCompraPendiente(espectaculo: EspectaculoDto, entrada: EntradaDisponible): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }

    const espectaculoSeleccionado: EspectaculoDto = {
      id: espectaculo.id,
      artista: espectaculo.artista,
      fecha: espectaculo.fecha,
      escenario: espectaculo.escenario,
    };

    sessionStorage.setItem(
      this.COMPRA_STORAGE_KEY,
      JSON.stringify({ espectaculo: espectaculoSeleccionado, entrada }),
    );
  }
}
