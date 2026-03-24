import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  toastMessage = signal<string | null>(null);

  private eventSource: EventSource | null = null;
  private clearTimer: ReturnType<typeof setTimeout> | null = null;

  connect(): void {
    if (this.eventSource) return;
    this.eventSource = new EventSource('http://localhost:8080/api/notifications/stream');

    const handler = (event: MessageEvent) => {
      this.toastMessage.set(event.data);
      if (this.clearTimer) clearTimeout(this.clearTimer);
      this.clearTimer = setTimeout(() => this.toastMessage.set(null), 5000);
    };

    this.eventSource.addEventListener('health-reminder', handler);
    this.eventSource.addEventListener('achievement', handler);
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
    if (this.clearTimer) clearTimeout(this.clearTimer);
  }
}
