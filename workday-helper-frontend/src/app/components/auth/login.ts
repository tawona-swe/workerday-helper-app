import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Mail, Lock, Eye, EyeOff, LogIn } from 'lucide-angular';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './login.html'
})
export class LoginComponent {
  email = '';
  password = '';
  showPassword = false;
  loading = false;
  error = '';

  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly Eye = Eye;
  readonly EyeOff = EyeOff;
  readonly LogIn = LogIn;

  constructor(private auth: AuthService) {}

  submit() {
    if (!this.email || !this.password) return;
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => {},
      error: (err) => {
        this.error = err.error?.error || 'Login failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
