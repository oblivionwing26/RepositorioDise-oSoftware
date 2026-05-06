import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Auth } from "../services/auth";

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})

export class ForgotPassword {
  email = '';
  sent = false;
  loading = false;

  constructor (private auth: Auth) {}

  submit(): void {
    this.loading = true;
    this.auth.forgotPassword(this.email).subscribe({
      next: () => { this.loading = false; this.sent = true; },
      error: () => { this.loading = false; this.sent = true; }
    });
  }
}

