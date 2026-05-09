import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../services/auth';

@Component({
  selector: 'app-mi-cuenta',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mi-cuenta.html',
  styleUrl: './mi-cuenta.css',
})
export class MiCuenta {
  error: string | null = null;
  mensaje: string | null = null;
  loading = false;

  constructor(
    public auth: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  cancelarCuenta(): void {
    const confirmado = confirm(
      '¿Seguro que quieres cancelar tu cuenta? Esta acción no se puede deshacer.'
    );

    if (!confirmado) {
      return;
    }

    this.error = null;
    this.mensaje = null;
    this.loading = true;

    this.auth.cancelAccount().subscribe({
      next: () => {
        this.loading = false;
        this.mensaje = 'Cuenta cancelada correctamente.';

        this.auth.logout();
        this.cdr.detectChanges();

        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1000);
      },
      error: err => {
        console.error('Error al cancelar cuenta:', err);

        this.loading = false;

        if (err.status === 401) {
          this.error = 'Tu sesión ha caducado. Vuelve a iniciar sesión.';
          this.auth.logout();
          this.router.navigate(['/login']);
          return;
        }

        this.error = 'No se pudo cancelar la cuenta.';
        this.cdr.detectChanges();
      },
    });
  }

  cerrarSesion(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}