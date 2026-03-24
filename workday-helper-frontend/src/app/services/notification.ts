import { Injectable, signal } from '@angular/core';
import { AuthService } from './auth';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  toastMessage = signal<string | null>(null);

  private eventSource: EventSource | null = null;
  private clearTimer: ReturnType<typeof setTimeout> | null = null;
  private audioCtx: AudioContext | null = null;

  constructor(private auth: AuthService) {}

  connect(): void {
    if (this.eventSource) return;
    const token = this.auth.getToken();
    if (!token) return;

    this.eventSource = new EventSource(
      `http://localhost:8080/api/notifications/stream?token=${encodeURIComponent(token)}`
    );

    const handler = (event: MessageEvent) => {
      let msg = event.data;
      try {
        const parsed = JSON.parse(event.data);
        msg = parsed.message ?? msg;
      } catch { /* raw string */ }

      this.showToast(msg);
      this.playBeep();
      this.showBrowserNotification(msg);
    };

    this.eventSource.addEventListener('health-reminder', handler);
    this.eventSource.addEventListener('achievement', handler);
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
    if (this.clearTimer) clearTimeout(this.clearTimer);
  }

  showToast(message: string): void {
    this.toastMessage.set(message);
    if (this.clearTimer) clearTimeout(this.clearTimer);
    this.clearTimer = setTimeout(() => this.toastMessage.set(null), 6000);
  }

  playBeep(): void {
    try {
      if (!this.audioCtx) {
        this.audioCtx = new AudioContext();
      }
      const ctx = this.audioCtx;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.4);
    } catch { /* audio not available */ }
  }

  showBrowserNotification(message: string): void {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'granted') {
      new Notification('Workday Helper', { body: message, icon: '/favicon.ico' });
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          new Notification('Workday Helper', { body: message, icon: '/favicon.ico' });
        }
      });
    }
  }

  requestNotificationPermission(): void {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }
}
