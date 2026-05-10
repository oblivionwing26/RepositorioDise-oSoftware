import { CommonModule } from "@angular/common";
import { ChangeDetectorRef, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { Auth } from "../services/auth";

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  email = '';
  password = '';
  confirm = '';
  error: string | null = null;
  loading = false;
  ok = false;

  constructor(
    private auth: Auth,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

 validClient(p: string): boolean {
    return p.length >= 8 && /[A-Za-z]/.test(p) && /\d/.test(p);
  }

  tieneLetraYNumero(p: string): boolean {
    return /[A-Za-z]/.test(p) && /\d/.test(p);
  }

  submit(emailValue = this.email, passwordValue = this.password, confirmValue = this.confirm): void {
    this.error = null;
    this.ok = false;

    this.email = emailValue.trim();
    this.password = passwordValue;
    this.confirm = confirmValue;

    if (!this.email || !this.password || !this.confirm) {
      this.error = 'Completa todos los campos.';
      return;
    }

    if (this.password !== this.confirm) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    if (!this.validClient(this.password)) {
      this.error = 'La contraseña debe tener al menos 8 caracteres, incluir letras y números.';
      return;
    }

    this.loading = true;

    this.auth.register(this.email, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.ok = true;
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/login']), 1200);
      },
      error: err => {
        console.error('Registro fallido:', err);

        this.loading = false;
        this.error = err.status === 0
          ? 'No se pudo conectar al servidor. Intenta más tarde.'
          : err.status === 409
            ? 'El correo ya está registrado.'
            : err.status === 400
              ? 'Datos inválidos. Revisa email y contraseña.'
              : 'Error al registrar la cuenta.';

        this.cdr.detectChanges();
      }
    });
  }
}
