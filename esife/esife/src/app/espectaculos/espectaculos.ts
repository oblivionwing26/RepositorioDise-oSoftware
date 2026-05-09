import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Auth } from '../services/auth';
import {
  EntradaDisponible,
  EscenarioDto,
  EspectaculoDto,
  EspectaculosService,
} from '../espectaculos';

interface ItemCarrito {
  espectaculo: EspectaculoDto;
  entrada: EntradaDisponible;
}

@Component({
  selector: 'espectaculos',
  imports: [CommonModule, FormsModule],
  templateUrl: './espectaculos.html',
  styleUrl: './espectaculos.css',
})
export class Espectaculos implements OnInit {
  carrito: ItemCarrito[] = [];
  mensajeCarrito: string | null = null;

  private readonly COMPRA_STORAGE_KEY = 'compraPendiente';
  escenarios: EscenarioDto[] = [];
  error: string | null = null;
  loadingEscenarios = false;
  busquedaArtista = '';
  filtroEscenario = '';
  filtroFecha = '';

  resultadosBusqueda: EspectaculoDto[] = [];
  busquedaRealizada = false;
  loadingBusqueda = false;

  constructor(
    private espectaculosService: EspectaculosService,
    private auth: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.getEscenarios();
    this.cargarCompraPendiente();
  }

  estaSeleccionada(entrada: EntradaDisponible): boolean {
    return this.carrito.some(item => item.entrada.id === entrada.id);
  }

  toggleEntrada(espectaculo: EspectaculoDto, entrada: EntradaDisponible): void {
    this.mensajeCarrito = null;

    if (this.estaSeleccionada(entrada)) {
      this.carrito = this.carrito.filter(item => item.entrada.id !== entrada.id);
      this.guardarCarrito();
      return;
    }

    if (this.carrito.length > 0 && this.carrito[0].espectaculo.id !== espectaculo.id) {
      this.mensajeCarrito = 'Solo puedes seleccionar entradas del mismo espectáculo en una compra.';
      return;
    }

    const espectaculoSeleccionado: EspectaculoDto = {
      id: espectaculo.id,
      artista: espectaculo.artista,
      fecha: espectaculo.fecha,
      escenario: espectaculo.escenario,
    };

    this.carrito = [
      ...this.carrito,
      {
        espectaculo: espectaculoSeleccionado,
        entrada,
      },
    ];

    this.guardarCarrito();
  }

  quitarDelCarrito(entrada: EntradaDisponible): void {
    this.carrito = this.carrito.filter(item => item.entrada.id !== entrada.id);
    this.guardarCarrito();
  }

  vaciarCarrito(): void {
    this.carrito = [];
    this.mensajeCarrito = null;

    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
    }
  }

  totalCarritoCentimos(): number {
    return this.carrito.reduce((total, item) => total + item.entrada.precio, 0);
  }

  comprarCarrito(): void {
    if (!this.carrito.length) {
      this.mensajeCarrito = 'Selecciona al menos una entrada antes de comprar.';
      return;
    }

    this.guardarCarrito();

    if (!this.auth.isLogged()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    const espectaculo = this.carrito[0].espectaculo;
    const entradas = this.carrito.map(item => item.entrada);

    this.router.navigate(['/comprar'], {
      state: {
        espectaculo,
        entrada: entradas[0],     // compatibilidad con la compra antigua
        entradas,                 // nueva compra múltiple
        items: this.carrito,
        total: this.totalCarritoCentimos(),
      },
    });
  }

  private guardarCarrito(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }

    if (!this.carrito.length) {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
      return;
    }

    const espectaculo = this.carrito[0].espectaculo;
    const entradas = this.carrito.map(item => item.entrada);

    sessionStorage.setItem(
      this.COMPRA_STORAGE_KEY,
      JSON.stringify({
        espectaculo,
        entrada: entradas[0],   // compatibilidad con código anterior
        entradas,
        items: this.carrito,
        total: this.totalCarritoCentimos(),
      }),
    );
  }

  private cargarCompraPendiente(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }

    const raw = sessionStorage.getItem(this.COMPRA_STORAGE_KEY);

    if (!raw) {
      return;
    }

    try {
      const data = JSON.parse(raw);

      if (Array.isArray(data.items)) {
        this.carrito = data.items;
        return;
      }

      if (data.espectaculo && data.entrada) {
        this.carrito = [
          {
            espectaculo: data.espectaculo,
            entrada: data.entrada,
          },
        ];
      }
    } catch {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
    }
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
  get escenariosFiltrados(): EscenarioDto[] {
    if (!this.filtroEscenario) {
      return this.escenarios;
    }

    return this.escenarios.filter(escenario =>
      escenario.nombre === this.filtroEscenario
    );
  }

  buscarEspectaculos(): void {
    const artista = this.busquedaArtista.trim();

    if (!artista) {
      this.resultadosBusqueda = [];
      this.busquedaRealizada = false;
      return;
    }

    this.error = null;
    this.loadingBusqueda = true;
    this.busquedaRealizada = true;

    this.espectaculosService.buscarEspectaculos(artista).subscribe({
      next: espectaculos => {
        this.resultadosBusqueda = espectaculos;
        this.loadingBusqueda = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron buscar espectáculos.';
        this.resultadosBusqueda = [];
        this.loadingBusqueda = false;
        this.cdr.detectChanges();
      },
    });
  }

  limpiarBusqueda(): void {
    this.busquedaArtista = '';
    this.filtroEscenario = '';
    this.filtroFecha = '';
    this.resultadosBusqueda = [];
    this.busquedaRealizada = false;
  }

  get resultadosFiltrados(): EspectaculoDto[] {
    return this.resultadosBusqueda.filter(espectaculo => {
      const coincideEscenario =
        !this.filtroEscenario ||
        espectaculo.escenario === this.filtroEscenario;

      const coincideFecha =
        !this.filtroFecha ||
        this.normalizarFecha(espectaculo.fecha) === this.filtroFecha;

      return coincideEscenario && coincideFecha;
    });
  }

  get escenariosDisponiblesFiltro(): string[] {
    return this.escenarios.map(escenario => escenario.nombre);
  }

  private normalizarFecha(fecha: string | Date | undefined): string {
    if (!fecha) {
      return '';
    }

    return String(fecha).slice(0, 10);
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
