import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';

import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('esife');

  constructor(private auth: AuthService, private router: Router) {}

  get isLogged(): boolean {
    return this.auth.isAuthenticated();
  }

  get userName(): string | null {
    return this.auth.getName() || this.auth.getEmail();
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }
}
