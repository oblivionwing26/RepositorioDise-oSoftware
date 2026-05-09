import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { Auth } from './services/auth';

@Component({  
  selector: 'app-root',
  imports: [CommonModule, RouterLink, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})


export class App {

  constructor(private auth: Auth, private router: Router) {}
  isLogged() { return this.auth.isLogged(); }
  logout() { this.auth.logout(); this.router.navigate(['/login']); }
  protected readonly title = signal('esife');
}
