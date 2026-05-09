import { CommonModule } from "@angular/common";
import { ChangeDetectorRef, Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { Auth } from "../services/auth";

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})

export class ForgotPassword {
  email = '';
  sent = false;
  loading = false;

  constructor (private auth: Auth, private cdr: ChangeDetectorRef) {}

  submit(): void {
    this.loading = true;
    this.auth.forgotPassword(this.email).subscribe({
      next: () => { this.loading = false; this.sent = true; this.cdr.detectChanges(); },
      error: () => { this.loading = false; this.sent = true; this.cdr.detectChanges(); }
    });
  }
}

