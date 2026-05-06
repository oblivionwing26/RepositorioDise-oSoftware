import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Pagos } from '../pagos';
import { Auth } from '../services/auth';
import { CompraResponse, ComprasService } from '../services/compras';
import { PrerreservaResponse, ReservasService } from '../services/reservas';
import { EntradaDisponible, EspectaculoDto } from '../espectaculos';

@Component({
  selector: 'app-compra',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './compra.html',
  styleUrl: './compra.css',
})
export class Compra implements OnInit {
  private readonly COMPRA_STORAGE_KEY = 'compraPendiente';
  clientSecret = '';
  importe = 0;
  entrada?: EntradaDisponible;
  espectaculo?: EspectaculoDto;
  prerreserva?: PrerreservaResponse;
  compra?: CompraResponse;
  mensaje: string | null = null;
  error: string | null = null;
  loading = false;
  loadingPrerreserva = false;

  constructor(
    private pagosService: Pagos,
    private comprasService: ComprasService,
    private reservasService: ReservasService,
    private auth: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    if (!this.auth.isLogged()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    const navigationState = history.state as {
      entrada?: EntradaDisponible;
      espectaculo?: EspectaculoDto;
      prerreserva?: PrerreservaResponse;
    };

    this.entrada = navigationState.entrada;
    this.espectaculo = navigationState.espectaculo;
    this.prerreserva = navigationState.prerreserva;

    if (!this.entrada) {
      this.cargarCompraPendiente();
    }

    if (this.prerreserva && !this.prerreservaActiva(this.prerreserva)) {
      this.prerreserva = undefined;
    }

    if (this.prerreserva?.precio != null) {
      this.importe = this.prerreserva.precio / 100;
    } else if (this.entrada?.precio != null) {
      this.importe = this.entrada.precio / 100;
    }

    if (this.entrada && !this.prerreserva) {
      this.crearPrerreserva();
    }
  }

  crearPrerreserva(): void {
    this.error = null;

    const tokenUsuario = this.auth.getToken();
    if (!tokenUsuario) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    if (!this.entrada) {
      this.error = 'Selecciona una entrada libre antes de prerreservar.';
      return;
    }

    this.loadingPrerreserva = true;
    this.reservasService.prerreservar(this.entrada.id, tokenUsuario).subscribe({
      next: prerreserva => {
        this.prerreserva = prerreserva;
        this.importe = prerreserva.precio / 100;
        this.loadingPrerreserva = false;
        this.guardarCompraPendiente();
        this.cdr.detectChanges();
      },
      error: err => {
        this.loadingPrerreserva = false;
        if (err.status === 401) {
          this.auth.logout();
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
          return;
        }
        this.error = this.mensajeError(err, 'No se pudo prerreservar la entrada.');
        this.cdr.detectChanges();
      },
    });
  }

  comprar(): void {
    this.error = null;
    this.mensaje = null;

    const tokenUsuario = this.auth.getToken();
    if (!tokenUsuario) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    if (!this.prerreserva?.tokenEntrada) {
      this.error = 'Primero hay que prerreservar la entrada.';
      return;
    }

    this.loading = true;
    this.comprasService.comprar(this.prerreserva.tokenEntrada, tokenUsuario).subscribe({
      next: compra => {
        this.compra = compra;
        this.mensaje = compra.mensaje;
        if (this.entrada) {
          this.entrada.estado = compra.estado;
        }
        this.limpiarCompraPendiente();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.loading = false;
        if (err.status === 401) {
          this.auth.logout();
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
          return;
        }
        this.error = this.mensajeError(err, 'No se pudo validar la compra.');
        this.cdr.detectChanges();
      },
    });
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
      const compraPendiente = JSON.parse(raw) as {
        entrada?: EntradaDisponible;
        espectaculo?: EspectaculoDto;
        prerreserva?: PrerreservaResponse;
      };
      this.entrada = compraPendiente.entrada;
      this.espectaculo = compraPendiente.espectaculo;
      if (compraPendiente.prerreserva && this.prerreservaActiva(compraPendiente.prerreserva)) {
        this.prerreserva = compraPendiente.prerreserva;
      }
    } catch {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
    }
  }

  private guardarCompraPendiente(): void {
    if (typeof sessionStorage === 'undefined' || !this.entrada) {
      return;
    }

    sessionStorage.setItem(
      this.COMPRA_STORAGE_KEY,
      JSON.stringify({
        entrada: this.entrada,
        espectaculo: this.espectaculo,
        prerreserva: this.prerreserva,
      }),
    );
  }

  private limpiarCompraPendiente(): void {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
    }
  }

  private prerreservaActiva(prerreserva: PrerreservaResponse): boolean {
    return new Date(prerreserva.expiraEn).getTime() > Date.now();
  }

  private mensajeError(err: any, fallback: string): string {
    if (typeof err.error === 'string') {
      return err.error;
    }
    return err.error?.message ?? fallback;
  }

  irAlPago(): void {
    const jsonData = {
      centimos: Math.floor(this.importe.valueOf() * 100),
    };

    this.pagosService.prepararPago(jsonData).subscribe({
      next: response => {
        this.clientSecret = response;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Error al preparar el pago.';
        this.cdr.detectChanges();
      },
    });
  }
}
