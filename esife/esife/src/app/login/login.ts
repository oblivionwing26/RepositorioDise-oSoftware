import { ChangeDetectorRef, Component } from '@angular/core';
import { Auth } from '../services/auth';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';



@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  email = '';
  password = '';
  error: string | null = null;
  loading = false;

  constructor(
    private auth: Auth,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {}

  submit(): void{
    this.error = null;
    this.loading = true;
    this.auth.login(this.email, this.password).subscribe({
      next: res => {
        this.auth.saveToken(res.token);
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl?.startsWith('/') ? returnUrl : '/');
      },
      error: err => {
        this.loading = false;
        this.error = err.status ===401
        ? 'Credenciales inválidas o cuenta desactivada.'
        : 'Error al iniciar sesión.';
        this.cdr.detectChanges();
      }
    });
  }

}
