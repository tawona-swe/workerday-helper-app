import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Mail, ArrowLeft, Send } from 'lucide-angular';

@Component({
  selector: 'app-forgot-password',
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './forgot-password.html'
})
export class ForgotPasswordComponent {
  email = '';
  submitted = false;

  readonly Mail = Mail;
  readonly ArrowLeft = ArrowLeft;
  readonly Send = Send;

  submit() {
    if (!this.email) return;
    // Stub — no email service wired up yet
    this.submitted = true;
  }
}
