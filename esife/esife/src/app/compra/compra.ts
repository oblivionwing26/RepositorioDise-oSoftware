import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { PagoPreparadoResponse, Pagos } from '../pagos';
import { Auth } from '../services/auth';
import { ColaService, TurnoColaResponse } from '../services/cola';
import { CompraResponse, ComprasService } from '../services/compras';
import { PrerreservaResponse, ReservasService } from '../services/reservas';
import { EntradaDisponible, EspectaculoDto } from '../espectaculos';

interface StripeCardChangeEvent {
  complete?: boolean;
  error?: { message?: string };
}

interface StripeCardElement {
  mount(target: string | HTMLElement): void;
  destroy(): void;
  on(event: 'change', handler: (event: StripeCardChangeEvent) => void): void;
}

interface StripeElements {
  create(type: 'card', options?: Record<string, unknown>): StripeCardElement;
}

interface StripePaymentIntent {
  id: string;
  status: string;
}

interface StripeConfirmResult {
  error?: { message?: string };
  paymentIntent?: StripePaymentIntent;
}

interface StripeInstance {
  elements(): StripeElements;
  confirmCardPayment(
    clientSecret: string,
    options: { payment_method: { card: StripeCardElement } },
  ): Promise<StripeConfirmResult>;
}

declare global {
  interface Window {
    Stripe?: (publicKey: string) => StripeInstance;
  }
}

@Component({
  selector: 'app-compra',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './compra.html',
  styleUrl: './compra.css',
})
export class Compra implements OnInit, OnDestroy {
  private readonly COMPRA_STORAGE_KEY = 'compraPendiente';
  private colaPollingId?: ReturnType<typeof setInterval>;
  private stripe?: StripeInstance;
  private stripeCard?: StripeCardElement;
  private stripeScriptPromise?: Promise<void>;
  clientSecret = '';
  importe = 0;
  entrada?: EntradaDisponible;
  espectaculo?: EspectaculoDto;
  prerreserva?: PrerreservaResponse;
  turnoCola?: TurnoColaResponse;
  pagoPreparado?: PagoPreparadoResponse;
  compra?: CompraResponse;
  mensaje: string | null = null;
  error: string | null = null;
  loading = false;
  loadingCola = false;
  loadingPrerreserva = false;
  loadingPago = false;
  confirmandoStripe = false;
  stripeCardComplete = false;
  stripeError: string | null = null;
  stripeMensaje: string | null = null;

  constructor(
    private pagosService: Pagos,
    private comprasService: ComprasService,
    private reservasService: ReservasService,
    private colaService: ColaService,
    private auth: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnDestroy(): void {
    this.detenerPollingCola();
    this.destruirFormularioStripe();
  }

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
      this.entrarEnColaOPrerreservar();
    }
  }

  entrarEnColaOPrerreservar(): void {
    if (this.turnoCola?.puedePrerreservar || this.turnoCola?.colaActiva === false) {
      this.crearPrerreserva();
      return;
    }

    this.entrarEnCola();
  }

  entrarEnCola(): void {
    this.error = null;

    const tokenUsuario = this.auth.getToken();
    if (!tokenUsuario) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
      return;
    }

    if (!this.espectaculo?.id) {
      this.error = 'No se pudo identificar el espectáculo para entrar en cola.';
      return;
    }

    this.loadingCola = true;
    this.colaService.entrar(this.espectaculo.id, tokenUsuario).subscribe({
      next: turno => this.procesarTurnoCola(turno),
      error: err => {
        this.loadingCola = false;
        if (err.status === 401) {
          this.auth.logout();
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/comprar' } });
          return;
        }
        this.error = this.mensajeError(err, 'No se pudo entrar en la cola virtual.');
        this.cdr.detectChanges();
      },
    });
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

    const idTurno = this.turnoCola?.idTurno;
    if (this.turnoCola?.colaActiva !== false && !idTurno) {
      this.error = 'Antes de prerreservar tienes que entrar en la cola virtual.';
      return;
    }

    this.loadingPrerreserva = true;
    this.reservasService.prerreservar(this.entrada.id, tokenUsuario, idTurno).subscribe({
      next: prerreserva => {
        this.prerreserva = prerreserva;
        this.importe = prerreserva.precio / 100;
        this.loadingPrerreserva = false;
        this.detenerPollingCola();
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

    if (!this.pagoPreparado?.tokenPago) {
      this.error = 'Prepara el pago antes de confirmar la compra.';
      return;
    }

    if (!this.pagoListoParaComprar()) {
      this.error = 'Completa el pago con Stripe antes de confirmar la compra.';
      return;
    }

    this.loading = true;
    this.comprasService.comprar(this.prerreserva.tokenEntrada, tokenUsuario, this.pagoPreparado.tokenPago).subscribe({
      next: compra => {
        this.compra = compra;
        this.mensaje = compra.mensaje;
        if (this.pagoPreparado) {
          this.pagoPreparado.estado = compra.estadoPago;
          this.pagoPreparado.metodo = compra.metodoPago;
        }
        if (this.entrada) {
          this.entrada.estado = compra.estado;
        }
        this.limpiarCompraPendiente();
        this.destruirFormularioStripe();
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
        turnoCola?: TurnoColaResponse;
        prerreserva?: PrerreservaResponse;
        pagoPreparado?: PagoPreparadoResponse;
      };
      this.entrada = compraPendiente.entrada;
      this.espectaculo = compraPendiente.espectaculo;
      this.turnoCola = compraPendiente.turnoCola;
      if (compraPendiente.prerreserva && this.prerreservaActiva(compraPendiente.prerreserva)) {
        this.prerreserva = compraPendiente.prerreserva;
        this.pagoPreparado = compraPendiente.pagoPreparado;
        this.clientSecret = compraPendiente.pagoPreparado?.clientSecret ?? '';
        if (this.esPagoStripePendiente()) {
          setTimeout(() => this.montarFormularioStripe(), 0);
        }
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
        turnoCola: this.turnoCola,
        prerreserva: this.prerreserva,
        pagoPreparado: this.pagoPreparado,
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

  private procesarTurnoCola(turno: TurnoColaResponse): void {
    this.turnoCola = turno;
    this.loadingCola = false;
    this.guardarCompraPendiente();

    if (!turno.colaActiva || turno.puedePrerreservar) {
      this.detenerPollingCola();
      this.crearPrerreserva();
      this.cdr.detectChanges();
      return;
    }

    if (turno.estado === 'ESPERANDO') {
      this.iniciarPollingCola();
      this.cdr.detectChanges();
      return;
    }

    this.detenerPollingCola();
    this.error = turno.mensaje || 'El turno de cola no esta disponible. Entra de nuevo en la cola.';
    this.cdr.detectChanges();
  }

  private iniciarPollingCola(): void {
    if (this.colaPollingId || typeof window === 'undefined') {
      return;
    }

    this.colaPollingId = setInterval(() => this.consultarEstadoCola(), 5000);
  }

  private consultarEstadoCola(): void {
    const tokenUsuario = this.auth.getToken();
    const idTurno = this.turnoCola?.idTurno;
    if (!tokenUsuario || !idTurno) {
      this.detenerPollingCola();
      return;
    }

    this.colaService.estado(idTurno, tokenUsuario).subscribe({
      next: turno => this.procesarTurnoCola(turno),
      error: err => {
        this.detenerPollingCola();
        this.error = this.mensajeError(err, 'No se pudo consultar el estado de la cola.');
        this.cdr.detectChanges();
      },
    });
  }

  private detenerPollingCola(): void {
    if (!this.colaPollingId) {
      return;
    }
    clearInterval(this.colaPollingId);
    this.colaPollingId = undefined;
  }

  private mensajeError(err: any, fallback: string): string {
    if (typeof err.error === 'string') {
      return err.error;
    }
    return err.error?.message ?? fallback;
  }

  irAlPago(): void {
    this.error = null;
    this.stripeError = null;
    this.stripeMensaje = null;

    if (!this.prerreserva) {
      this.error = 'Primero hay que prerreservar la entrada.';
      return;
    }

    const jsonData = {
      centimos: Math.floor(this.importe.valueOf() * 100),
    };

    this.loadingPago = true;
    this.pagosService.prepararPago(jsonData).subscribe({
      next: pago => {
        this.pagoPreparado = pago;
        this.clientSecret = pago.clientSecret;
        this.loadingPago = false;
        this.guardarCompraPendiente();
        if (this.esPagoStripePendiente()) {
          setTimeout(() => this.montarFormularioStripe(), 0);
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingPago = false;
        this.error = 'Error al preparar el pago.';
        this.cdr.detectChanges();
      },
    });
  }

  pagoListoParaComprar(): boolean {
    if (!this.pagoPreparado) {
      return false;
    }
    return !this.esPagoStripe() || this.pagoPreparado.estado === 'CONFIRMADO';
  }

  esPagoStripe(): boolean {
    return this.pagoPreparado?.metodo === 'STRIPE';
  }

  async confirmarPagoStripe(): Promise<void> {
    this.error = null;
    this.stripeError = null;
    this.stripeMensaje = null;

    if (!this.pagoPreparado || !this.clientSecret || !this.stripe || !this.stripeCard) {
      this.stripeError = 'El formulario de Stripe no esta listo.';
      return;
    }

    this.confirmandoStripe = true;
    this.cdr.detectChanges();

    try {
      const result = await this.stripe.confirmCardPayment(this.clientSecret, {
        payment_method: { card: this.stripeCard },
      });

      if (result.error) {
        this.stripeError = result.error.message ?? 'Stripe ha rechazado el pago.';
        return;
      }

      if (result.paymentIntent?.status !== 'succeeded') {
        this.stripeError = 'Stripe todavia no ha completado el pago.';
        return;
      }

      this.pagoPreparado.estado = 'CONFIRMADO';
      this.pagoPreparado.tokenPago = result.paymentIntent.id;
      this.stripeMensaje = 'Pago confirmado por Stripe.';
      this.guardarCompraPendiente();
    } catch {
      this.stripeError = 'No se pudo confirmar el pago con Stripe.';
    } finally {
      this.confirmandoStripe = false;
      this.cdr.detectChanges();
    }
  }

  private esPagoStripePendiente(): boolean {
    return this.esPagoStripe() && this.pagoPreparado?.estado !== 'CONFIRMADO';
  }

  private montarFormularioStripe(): void {
    if (!this.pagoPreparado?.publicKey) {
      this.stripeError = 'Falta la clave publica de Stripe.';
      this.cdr.detectChanges();
      return;
    }

    this.destruirFormularioStripe();
    this.stripeCardComplete = false;

    this.cargarStripe(this.pagoPreparado.publicKey)
      .then(stripe => {
        this.stripe = stripe;
        const elements = stripe.elements();
        this.stripeCard = elements.create('card', { hidePostalCode: true });
        this.stripeCard.mount('#stripe-card-element');
        this.stripeCard.on('change', event => {
          this.stripeCardComplete = !!event.complete;
          this.stripeError = event.error?.message ?? null;
          this.cdr.detectChanges();
        });
      })
      .catch(() => {
        this.stripeError = 'No se pudo cargar Stripe.';
        this.cdr.detectChanges();
      });
  }

  private cargarStripe(publicKey: string): Promise<StripeInstance> {
    if (typeof window === 'undefined' || typeof document === 'undefined') {
      return Promise.reject(new Error('Stripe solo se puede cargar en navegador'));
    }

    if (window.Stripe) {
      return Promise.resolve(window.Stripe(publicKey));
    }

    if (!this.stripeScriptPromise) {
      this.stripeScriptPromise = new Promise<void>((resolve, reject) => {
        const existing = document.querySelector<HTMLScriptElement>('script[src="https://js.stripe.com/v3/"]');
        if (existing) {
          existing.addEventListener('load', () => resolve(), { once: true });
          existing.addEventListener('error', () => reject(new Error('No se pudo cargar Stripe')), { once: true });
          return;
        }

        const script = document.createElement('script');
        script.src = 'https://js.stripe.com/v3/';
        script.async = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('No se pudo cargar Stripe'));
        document.head.appendChild(script);
      });
    }

    return this.stripeScriptPromise.then(() => {
      if (!window.Stripe) {
        throw new Error('Stripe no esta disponible');
      }
      return window.Stripe(publicKey);
    });
  }

  private destruirFormularioStripe(): void {
    if (!this.stripeCard) {
      return;
    }
    this.stripeCard.destroy();
    this.stripeCard = undefined;
  }
}
