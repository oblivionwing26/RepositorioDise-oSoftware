import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  email = '';
  name = '';
  password = '';
  confirm = '';
  loading = false;
  error: string | null = null;
  success = false;

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.error = null;
    this.success = false;

    if (!this.email || !this.password) {
      this.error = 'Email y contraseña son obligatorios.';
      return;
    }
    if (this.password !== this.confirm) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    if (!this.isStrong(this.password)) {
      this.error =
        'La contraseña debe tener al menos 10 caracteres e incluir mayúscula, minúscula, dígito y carácter especial.';
      return;
    }

    this.loading = true;
    this.auth
      .register({ email: this.email, name: this.name, password: this.password })
      .subscribe({
        next: () => {
          this.loading = false;
          this.success = true;
          setTimeout(() => this.router.navigateByUrl('/login'), 800);
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          this.error = err?.error?.message ?? 'No se pudo completar el registro.';
        },
      });
  }

  private isStrong(pwd: string): boolean {
    return (
      pwd.length >= 10 &&
      /[a-z]/.test(pwd) &&
      /[A-Z]/.test(pwd) &&
      /\d/.test(pwd) &&
      /[!-/:-@\[-`{-~]/.test(pwd) &&
      !/\s/.test(pwd)
    );
  }
}
