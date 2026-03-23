import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Reminder } from '../models/reminder.model';

@Injectable({ providedIn: 'root' })
export class ReminderService {
  private baseUrl = 'http://localhost:8080/api/reminders';
  private timers: Map<number, ReturnType<typeof setInterval>> = new Map();
  reminderTriggered$ = new Subject<Reminder>();

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

  startTimers(reminders: Reminder[]): void {
    this.stopAllTimers();
    reminders.filter(r => r.active).forEach(r => this.startTimer(r));
  }

  startTimer(reminder: Reminder): void {
    if (!reminder.id) return;
    const ms = reminder.intervalMinutes * 60 * 1000;
    const timer = setInterval(() => {
      this.zone.run(() => this.reminderTriggered$.next(reminder));
      this.trigger(reminder.id!).subscribe();
    }, ms);
    this.timers.set(reminder.id, timer);
  }

  stopTimer(id: number): void {
    const t = this.timers.get(id);
    if (t) { clearInterval(t); this.timers.delete(id); }
  }

  stopAllTimers(): void {
    this.timers.forEach(t => clearInterval(t));
    this.timers.clear();
  }
}
