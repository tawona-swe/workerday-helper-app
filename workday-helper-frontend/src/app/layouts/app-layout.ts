import { Component, HostListener, ViewChild, ElementRef, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule, Eye, PersonStanding, Droplets, Target, Settings, User, LogOut } from 'lucide-angular';
import { AuthService } from '../services/auth';
import { NotificationService } from '../services/notification';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './app-layout.html'
})
export class AppLayoutComponent implements OnInit {
  readonly Eye = Eye;
  readonly PersonStanding = PersonStanding;
  readonly Droplets = Droplets;
  readonly Target = Target;
  readonly Settings = Settings;
  readonly UserIcon = User;
  readonly LogOut = LogOut;

  dropdownOpen = false;
  dropdownTop = 0;
  dropdownRight = '16px';

  @ViewChild('avatarBtn') avatarBtn!: ElementRef<HTMLButtonElement>;

  readonly todayLabel = new Date().toLocaleDateString('en-US', {
    weekday: 'long', month: 'long', year: 'numeric'
  });

  constructor(public auth: AuthService, private notifications: NotificationService) {}

  ngOnInit(): void {
    this.notifications.connect();
    this.notifications.requestNotificationPermission();
  }

  userInitial(): string {
    const name = this.auth.currentUser()?.name;
    return name ? name[0].toUpperCase() : 'U';
  }

  firstName(): string {
    const name = this.auth.currentUser()?.name;
    return name ? name.split(' ')[0] : 'there';
  }

  toggleDropdown(event: MouseEvent) {
    this.dropdownOpen = !this.dropdownOpen;
    if (this.dropdownOpen) {
      const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
      this.dropdownTop = rect.bottom + 8;
      this.dropdownRight = `${window.innerWidth - rect.right}px`;
    }
  }

  logout() {
    this.dropdownOpen = false;
    this.auth.logout();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!(e.target as HTMLElement).closest('.avatar-dropdown')) {
      this.dropdownOpen = false;
    }
  }
}
