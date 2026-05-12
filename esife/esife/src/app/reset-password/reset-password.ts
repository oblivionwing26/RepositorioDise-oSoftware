import { CommonModule, isPlatformBrowser, Location } from "@angular/common";
import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID } from "@angular/core";
import { FormsModule } from "@angular/forms";

import { ActivatedRoute, Router } from "@angular/router";
import { Auth } from "../services/auth";

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPassword implements OnInit {
  token = '';
  tokenFromLink = false;
  newPassword = ''
  confirm = '';
  error: string | null = null;
  ok = false;
  loading = false;
  mostrarPassword = false;
  mostrarConfirm = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: Auth,
    private cdr: ChangeDetectorRef,
    private location: Location,
    @Inject(PLATFORM_ID) private platformId: object,
  ) {}

  ngOnInit(): void {
    this.token = this.readTokenFromLink();
    this.tokenFromLink = this.token.length > 0;

    if (this.tokenFromLink && isPlatformBrowser(this.platformId)) {
      this.location.replaceState('/reset-password');
    }
  }

  private readTokenFromLink(): string {
    const queryToken = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';
    const fragmentToken = this.tokenFromFragment(this.route.snapshot.fragment);
    const browserToken = this.tokenFromBrowserUrl();

    return queryToken || fragmentToken || browserToken;
  }

  private tokenFromBrowserUrl(): string {
    if (!isPlatformBrowser(this.platformId)) {
      return '';
    }

    const url = new URL(window.location.href);
    const queryToken = url.searchParams.get('token')?.trim() ?? '';

    return queryToken || this.tokenFromFragment(url.hash);
  }

  private tokenFromFragment(fragment: string | null): string {
    if (!fragment) {
      return '';
    }

    const withoutHash = fragment.startsWith('#') ? fragment.substring(1) : fragment;
    const cleanFragment = withoutHash.startsWith('?') ? withoutHash.substring(1) : withoutHash;
    return new URLSearchParams(cleanFragment).get('token')?.trim() ?? '';
  }

  private validClient(p: string): boolean{
    return p.length >= 8 && /[A-Za-z]/.test(p) && /\d/.test(p);
  }

  submit(tokenValue = this.token, passwordValue = this.newPassword, confirmValue = this.confirm): void {
    this.error = null;

    this.token = tokenValue.trim();
    this.newPassword = passwordValue;
    this.confirm = confirmValue;
    
    if(!this.token) {this.error = 'Enlace de recuperación inválido o incompleto.'; return;}
    if (this.newPassword !== this.confirm) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    if (!this.validClient(this.newPassword)) {
      this.error = 'La contraseña debe tener al menos 8 caracteres, incluir letras y números.';
      return;
    }
    this.loading = true;
    this.auth.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.ok = true;
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/login']), 1200);
      },
      error: err => {
        this.loading = false;
        this.error = err.status === 401
          ? 'Token inválido o expirado.'
          : 'No se pudo restablecer la contraseña.';
        this.cdr.detectChanges();
      }
    });
  } 
}