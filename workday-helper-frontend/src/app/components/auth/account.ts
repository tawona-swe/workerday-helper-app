import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, User, Mail, Lock, Eye, EyeOff, Save, CheckCircle } from 'lucide-angular';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-account',
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './account.html'
})
export class AccountComponent {
  name = '';
  newPassword = '';
  showPassword = false;
  loading = false;
  success = '';
  error = '';

  readonly User = User;
  readonly Mail = Mail;
  readonly Lock = Lock;
  readonly Eye = Eye;
  readonly EyeOff = EyeOff;
  readonly Save = Save;
  readonly CheckCircle = CheckCircle;

  constructor(public auth: AuthService) {
    this.name = auth.currentUser()?.name ?? '';
  }

  save() {
    this.loading = true;
    this.success = '';
    this.error = '';
    const payload: { name?: string; password?: string } = {};
    if (this.name) payload.name = this.name;
    if (this.newPassword) payload.password = this.newPassword;

    this.auth.updateProfile(payload).subscribe({
      next: () => {
        this.success = 'Profile updated successfully.';
        this.newPassword = '';
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Update failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
