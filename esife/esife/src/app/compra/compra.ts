import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Pagos } from '../pagos';
import { Auth } from '../services/auth';
import { ComprasService } from '../services/compras';
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
  mensaje: string | null = null;
  error: string | null = null;
  loading = false;

  constructor(
    private pagosService: Pagos,
    private comprasService: ComprasService,
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
    };

    this.entrada = navigationState.entrada;
    this.espectaculo = navigationState.espectaculo;

    if (!this.entrada) {
      this.cargarCompraPendiente();
    }

    if (this.entrada?.precio != null) {
      this.importe = this.entrada.precio / 100;
    }
  }

  comprar(): void {
    this.error = null;
    this.mensaje = null;

    const tokenUsuario = this.auth.getToken();
    if (!tokenUsuario) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    if (!this.entrada) {
      this.error = 'Selecciona una entrada libre antes de comprar.';
      return;
    }

    this.loading = true;
    this.comprasService.comprar(String(this.entrada.id), tokenUsuario).subscribe({
      next: mensaje => {
        this.mensaje = mensaje;
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
        this.error = err.error?.message ?? 'No se pudo validar la compra.';
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
      };
      this.entrada = compraPendiente.entrada;
      this.espectaculo = compraPendiente.espectaculo;
    } catch {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
    }
  }

  private limpiarCompraPendiente(): void {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(this.COMPRA_STORAGE_KEY);
    }
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
