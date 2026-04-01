import { Component, OnInit } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { CalendarService, CalendarEvent } from '../../services/calendar';

interface TaskSuggestion {
  taskId: number;
  title: string;
  priority: string;
  suggestedStartTime: string;
  suggestedEndTime: string;
  rationale: string;
  aiAdvice?: string;
  conflict?: boolean;
}

interface ContextSuggestion {
  taskId: number;
  title: string;
  suggestedStartTime: string;
  suggestedEndTime: string;
  rationale: string;
}

import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-suggestions',
  standalone: true,
  imports: [CommonModule, NgClass],
  templateUrl: './suggestions.html'
})
export class SuggestionsComponent implements OnInit {
  private readonly base = `${environment.apiUrl}/api/suggestions`;

  daily: TaskSuggestion[] = [];
  context: ContextSuggestion[] = [];
  loading = true;
  calendarConnected = false;
  calendarEvents: CalendarEvent[] = [];

  constructor(private http: HttpClient, private calendarService: CalendarService) {}

  ngOnInit(): void {
    let done = 0;
    const finish = () => { if (++done === 2) this.loading = false; };

    // Load calendar events first, then pass free slots to suggestions
    this.calendarService.getStatus().subscribe(s => {
      this.calendarConnected = s.connected;
      if (s.connected) {
        this.calendarService.getEvents().subscribe(events => {
          this.calendarEvents = events;
          this.loadSuggestions(finish);
        });
      } else {
        this.loadSuggestions(finish);
      }
    });
  }

  private loadSuggestions(finish: () => void): void {
    this.http.get<TaskSuggestion[]>(`${this.base}/daily`).subscribe({
      next: d => {
        // Flag suggestions that conflict with calendar events
        this.daily = d.map(s => ({
          ...s,
          conflict: this.hasConflict(s.suggestedStartTime, s.suggestedEndTime)
        }));
        finish();
      },
      error: () => finish()
    });
    this.http.get<ContextSuggestion[]>(`${this.base}/context`).subscribe({
      next: c => { this.context = c; finish(); },
      error: () => finish()
    });
  }

  private hasConflict(start: string, end: string): boolean {
    if (!this.calendarEvents.length) return false;
    const s = this.parseTime(start);
    const e = this.parseTime(end);
    return this.calendarEvents.some(ev => {
      const evStart = new Date(ev.start).getHours() * 60 + new Date(ev.start).getMinutes();
      const evEnd = new Date(ev.end).getHours() * 60 + new Date(ev.end).getMinutes();
      return s < evEnd && e > evStart;
    });
  }

  private parseTime(t: string): number {
    const [h, m] = t.split(':').map(Number);
    return h * 60 + (m || 0);
  }

  priorityColor(priority: string): string {
    if (priority === 'HIGH') return 'bg-red-100 text-red-700';
    if (priority === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-gray-100 text-gray-500';
  }
}
