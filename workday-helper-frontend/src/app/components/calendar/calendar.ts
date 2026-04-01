import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CalendarService, CalendarEvent } from '../../services/calendar';
import { TaskService } from '../../services/task';
import { Task } from '../../models/task.model';

@Component({
  selector: 'app-calendar',
  imports: [CommonModule],
  templateUrl: './calendar.html'
})
export class CalendarComponent implements OnInit {
  connected = false;
  events: CalendarEvent[] = [];
  tasks: Task[] = [];
  loading = true;
  syncingTaskId: number | null = null;
  syncedTaskIds = new Set<number>();

  constructor(private calendarService: CalendarService, private taskService: TaskService) {}

  ngOnInit(): void {
    // Check for callback result in URL
    const params = new URLSearchParams(window.location.search);
    if (params.get('calendar') === 'connected') {
      window.history.replaceState({}, '', window.location.pathname);
    }

    this.calendarService.getStatus().subscribe(s => {
      this.connected = s.connected;
      if (this.connected) this.loadEvents();
      else this.loading = false;
    });

    this.taskService.getAll().subscribe(t => {
      this.tasks = t.filter(task => !task.completed && task.dueDate);
    });
  }

  connect(): void {
    this.calendarService.getAuthUrl().subscribe(res => {
      window.location.href = res.url;
    });
  }

  disconnect(): void {
    this.calendarService.disconnect().subscribe(() => {
      this.connected = false;
      this.events = [];
    });
  }

  loadEvents(): void {
    this.loading = true;
    this.calendarService.getEvents().subscribe({
      next: e => { this.events = e; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  syncTask(task: Task): void {
    this.syncingTaskId = task.id!;
    this.calendarService.syncTask(task.id!).subscribe({
      next: () => { this.syncedTaskIds.add(task.id!); this.syncingTaskId = null; this.loadEvents(); },
      error: () => { this.syncingTaskId = null; }
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('en-US', {
      weekday: 'short', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
