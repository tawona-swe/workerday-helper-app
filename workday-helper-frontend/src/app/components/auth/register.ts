import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Mail, Lock, Eye, EyeOff, User, UserPlus } from 'lucide-angular';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './register.html'
})
export class RegisterComponent {
  name = '';
  email = '';
  password = '';
  showPassword = false;
  loading = false;
  error = '';

  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly Eye = Eye;
  readonly EyeOff = EyeOff;
  readonly User = User;
  readonly UserPlus = UserPlus;

  constructor(private auth: AuthService) {}

  submit() {
    if (!this.name || !this.email || !this.password) return;
    this.loading = true;
    this.error = '';
    this.auth.register(this.name, this.email, this.password).subscribe({
      next: () => {},
      error: (err) => {
        this.error = err.error?.error || 'Registration failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
