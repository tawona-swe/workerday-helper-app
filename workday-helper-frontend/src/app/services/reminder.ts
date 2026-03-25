import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Reminder } from '../models/reminder.model';

@Injectable({ providedIn: 'root' })
export class ReminderService {
  private baseUrl = 'http://localhost:8080/api/reminders';
  private timers: Map<number, ReturnType<typeof setTimeout>> = new Map();
  reminderTriggered$ = new Subject<Reminder>();
  private initialized = false;

  constructor(private http: HttpClient, private zone: NgZone) {}

  getAll(): Observable<Reminder[]> {
    return this.http.get<Reminder[]>(this.baseUrl);
  }

  create(reminder: Reminder): Observable<Reminder> {
    return this.http.post<Reminder>(this.baseUrl, reminder);
  }

  update(reminder: Reminder): Observable<Reminder> {
    return this.http.put<Reminder>(`${this.baseUrl}/${reminder.id}`, reminder);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  trigger(id: number): Observable<Reminder> {
    return this.http.patch<Reminder>(`${this.baseUrl}/${id}/trigger`, {});
  }

  /** Call once at app startup — schedules all active reminders respecting lastTriggered */
  initTimers(): void {
    if (this.initialized) return;
    this.initialized = true;
    this.getAll().subscribe(reminders => {
      this.stopAllTimers();
      reminders.filter(r => r.active).forEach(r => this.scheduleTimer(r));
    });
  }

  /** Refresh timers after create/update/delete — resets only changed reminders */
  refreshTimers(): void {
    this.getAll().subscribe(reminders => {
      this.stopAllTimers();
      reminders.filter(r => r.active).forEach(r => this.scheduleTimer(r));
    });
  }

  private scheduleTimer(reminder: Reminder): void {
    if (!reminder.id) return;
    const intervalMs = reminder.intervalMinutes * 60 * 1000;

    // Calculate how long until next fire, accounting for lastTriggered
    let delayMs = intervalMs;
    if (reminder.lastTriggered) {
      const elapsed = Date.now() - new Date(reminder.lastTriggered).getTime();
      delayMs = Math.max(0, intervalMs - elapsed);
    }

    const fire = () => {
      this.zone.run(() => this.reminderTriggered$.next(reminder));
      this.trigger(reminder.id!).subscribe();
      // Schedule next occurrence
      const next = setTimeout(fire, intervalMs);
      this.timers.set(reminder.id!, next);
    };

    const t = setTimeout(fire, delayMs);
    this.timers.set(reminder.id, t);
  }

  stopTimer(id: number): void {
    const t = this.timers.get(id);
    if (t) { clearTimeout(t); this.timers.delete(id); }
  }

  stopAllTimers(): void {
    this.timers.forEach(t => clearTimeout(t));
    this.timers.clear();
  }
}
