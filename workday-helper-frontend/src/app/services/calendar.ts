import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CalendarEvent {
  id: string;
  title: string;
  start: string;
  end: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private base = `${environment.apiUrl}/api/calendar`;

  constructor(private http: HttpClient) {}

  getStatus(): Observable<{ connected: boolean }> {
    return this.http.get<{ connected: boolean }>(`${this.base}/status`);
  }

  getAuthUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/auth-url`);
  }

  getEvents(): Observable<CalendarEvent[]> {
    return this.http.get<CalendarEvent[]>(`${this.base}/events`);
  }

  syncTask(taskId: number): Observable<{ eventId: string; status: string }> {
    return this.http.post<{ eventId: string; status: string }>(`${this.base}/sync-task/${taskId}`, {});
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${this.base}/disconnect`);
  }
}
