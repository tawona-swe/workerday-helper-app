import { Component, HostListener, ViewChild, ElementRef, OnInit, AfterViewChecked } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { LucideAngularModule, Eye, PersonStanding, Droplets, Target, Settings, User, LogOut } from 'lucide-angular';
import { AuthService } from '../services/auth';
import { NotificationService } from '../services/notification';
import { TaskService } from '../services/task';
import { ReminderService } from '../services/reminder';
import { Task } from '../models/task.model';

interface ChatMsg { role: string; content: string; }
interface NotifItem { message: string; time: string; }

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule, CommonModule, FormsModule],
  templateUrl: './app-layout.html'
})
export class AppLayoutComponent implements OnInit, AfterViewChecked {
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

  // Search
  searchOpen = false;
  searchQuery = '';
  searchResults: Task[] = [];
  searchTop = 0;
  searchRight = '0px';
  private allTasks: Task[] = [];

  // Notifications
  notifOpen = false;
  notifList: NotifItem[] = [];
  unreadCount = 0;
  notifTop = 0;
  notifRight = '0px';

  // Mini chat popup
  chatOpen = false;
  chatMessages: ChatMsg[] = [];
  chatInput = '';
  chatSending = false;
  private shouldScroll = false;

  @ViewChild('avatarBtn') avatarBtn!: ElementRef<HTMLButtonElement>;
  @ViewChild('chatBody') chatBody!: ElementRef<HTMLDivElement>;
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;
  @ViewChild('searchBtn') searchBtn!: ElementRef<HTMLButtonElement>;
  @ViewChild('notifBtn') notifBtn!: ElementRef<HTMLButtonElement>;

  readonly todayLabel = new Date().toLocaleDateString('en-US', {
    weekday: 'long', month: 'long', year: 'numeric'
  });

  constructor(
    public auth: AuthService,
    public notifications: NotificationService,
    private http: HttpClient,
    private taskService: TaskService,
    private reminderService: ReminderService,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.notifications.connect();
    this.notifications.requestNotificationPermission();
    this.taskService.getAll().subscribe(t => this.allTasks = t);
    // Start reminder timers globally — fires notifications from any page
    this.reminderService.initTimers();
    this.reminderService.reminderTriggered$.subscribe(r => {
      this.notifications.showToast(r.message);
      this.notifications.playBeep();
      this.notifications.showBrowserNotification(r.message);
    });
    this.notifications.onNotification = (msg: string) => {
      this.notifList.unshift({ message: msg, time: new Date().toLocaleTimeString() });
      this.unreadCount++;
    };
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll && this.chatBody) {
      this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      this.shouldScroll = false;
    }
  }

  userInitial(): string {
    const name = this.auth.currentUser()?.name;
    return name ? name[0].toUpperCase() : 'U';
  }

  firstName(): string {
    const name = this.auth.currentUser()?.name;
    return name ? name.split(' ')[0] : 'there';
  }

  // Search
  onSearch(): void {
    const q = this.searchQuery.toLowerCase().trim();
    this.searchResults = q.length > 1
      ? this.allTasks.filter(t => t.title.toLowerCase().includes(q)).slice(0, 6)
      : [];
  }

  focusSearch(): void {
    setTimeout(() => this.searchInput?.nativeElement?.focus(), 50);
  }

  toggleSearch(event: MouseEvent): void {
    this.searchOpen = !this.searchOpen;
    if (this.searchOpen) {
      const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
      this.searchTop = rect.bottom + 8;
      this.searchRight = `${window.innerWidth - rect.right}px`;
      this.focusSearch();
    } else {
      this.searchQuery = '';
      this.searchResults = [];
    }
  }

  // Notifications
  clearNotifs(): void {
    this.notifList = [];
    this.unreadCount = 0;
  }

  toggleNotif(event: MouseEvent): void {
    this.notifOpen = !this.notifOpen;
    this.unreadCount = 0;
    if (this.notifOpen) {
      const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
      this.notifTop = rect.bottom + 8;
      this.notifRight = `${window.innerWidth - rect.right}px`;
    }
  }

  // Chat popup
  toggleChat(): void {
    this.chatOpen = !this.chatOpen;
    if (this.chatOpen && this.chatMessages.length === 0) {
      this.http.get<ChatMsg[]>('http://localhost:8080/api/assistant/history').subscribe({
        next: msgs => { this.chatMessages = msgs; this.shouldScroll = true; },
        error: () => {}
      });
    }
  }

  sendChat(): void {
    const text = this.chatInput.trim();
    if (!text || this.chatSending) return;
    this.chatMessages.push({ role: 'user', content: text });
    this.chatInput = '';
    this.chatSending = true;
    this.shouldScroll = true;
    this.http.post<ChatMsg>('http://localhost:8080/api/assistant/message', { message: text }).subscribe({
      next: reply => {
        this.chatMessages.push(reply);
        this.chatSending = false;
        this.shouldScroll = true;
      },
      error: () => {
        this.chatMessages.push({ role: 'assistant', content: 'Error getting response.' });
        this.chatSending = false;
        this.shouldScroll = true;
      }
    });
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
    const target = e.target as HTMLElement;
    if (!target.closest('.avatar-dropdown')) this.dropdownOpen = false;
    if (!target.closest('.search-panel') && !target.closest('.search-portal')) {
      this.searchOpen = false; this.searchResults = []; this.searchQuery = '';
    }
    if (!target.closest('.notif-panel') && !target.closest('.notif-portal')) this.notifOpen = false;
  }
}
