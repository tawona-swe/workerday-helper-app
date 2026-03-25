import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { LucideAngularModule, Eye, PersonStanding, Droplets, Dumbbell, Bell, Trash2, LucideIconData } from 'lucide-angular';
import { ReminderService } from '../../services/reminder';
import { NotificationService } from '../../services/notification';
import { Reminder } from '../../models/reminder.model';

@Component({
  selector: 'app-reminders',
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './reminders.html',
  styleUrl: './reminders.scss'
})
export class RemindersComponent implements OnInit, OnDestroy {
  reminders: Reminder[] = [];
  showForm = false;
  activeAlert: Reminder | null = null;
  private sub!: Subscription;

  newReminder: Reminder = { message: '', type: 'CUSTOM', intervalMinutes: 20, active: true };

  readonly Eye = Eye;
  readonly PersonStanding = PersonStanding;
  readonly Droplets = Droplets;
  readonly Dumbbell = Dumbbell;
  readonly Bell = Bell;
  readonly Trash2 = Trash2;

  typeIconMap: Record<string, LucideIconData> = {
    EYE_BREAK: Eye, POSTURE: PersonStanding, HYDRATION: Droplets, STRETCH: Dumbbell, CUSTOM: Bell
  };

  // keep string map for select option labels only
  typeLabels: Record<string, string> = {
    EYE_BREAK: 'Eye Break', POSTURE: 'Posture Check', HYDRATION: 'Hydration', STRETCH: 'Stretch', CUSTOM: 'Custom'
  };

  constructor(private reminderService: ReminderService, private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.load();
    this.sub = this.reminderService.reminderTriggered$.subscribe(r => {
      this.activeAlert = r;
      this.notificationService.playBeep();
      this.notificationService.showBrowserNotification(r.message);
      setTimeout(() => this.activeAlert = null, 8000);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  load(): void {
    this.reminderService.getAll().subscribe(r => {
      this.reminders = r;
    });
  }

  save(): void {
    if (!this.newReminder.message.trim()) return;
    this.reminderService.create(this.newReminder).subscribe(() => {
      this.reminderService.refreshTimers();
      this.load(); this.cancel();
    });
  }

  toggle(reminder: Reminder): void {
    const updated = { ...reminder, active: !reminder.active };
    this.reminderService.update(updated).subscribe(() => {
      this.reminderService.refreshTimers();
      this.load();
    });
  }

  delete(id: number): void {
    this.reminderService.stopTimer(id);
    this.reminderService.delete(id).subscribe(() => this.load());
  }

  cancel(): void {
    this.showForm = false;
    this.newReminder = { message: '', type: 'CUSTOM', intervalMinutes: 20, active: true };
  }

  dismissAlert(): void {
    this.activeAlert = null;
  }

  get activeCount(): number { return this.reminders.filter(r => r.active).length; }
}
