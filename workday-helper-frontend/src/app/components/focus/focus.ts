import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface FocusSession {
  id: number;
  taskId?: number;
  targetDurationMinutes?: number;
  startTime?: string;
  endTime?: string;
  status?: string;
}

interface FocusSummary {
  totalSessions: number;
  totalFocusMinutes: number;
  averageSessionMinutes: number;
}

import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-focus',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './focus.html'
})
export class FocusComponent implements OnInit, OnDestroy {
  private readonly base = `${environment.apiUrl}/api/focus`;

  activeSession: FocusSession | null = null;
  summary: FocusSummary | null = null;
  taskId = '';
  duration = 25;
  secondsLeft = 0;
  private timer: ReturnType<typeof setInterval> | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadActive();
    this.loadSummary();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  loadActive(): void {
    this.http.get<FocusSession | null>(`${this.base}/active`).subscribe({
      next: s => {
        this.activeSession = s;
        if (s?.targetDurationMinutes) this.startCountdown(s.targetDurationMinutes);
      },
      error: () => {}
    });
  }

  loadSummary(): void {
    this.http.get<FocusSummary>(`${this.base}/summary`).subscribe({
      next: s => this.summary = s,
      error: () => {}
    });
  }

  startSession(): void {
    const body: any = {};
    if (this.taskId) body['taskId'] = Number(this.taskId);
    if (this.duration) body['targetDurationMinutes'] = this.duration;
    this.http.post<FocusSession>(`${this.base}/start`, body).subscribe({
      next: s => {
        this.activeSession = s;
        if (s.targetDurationMinutes) this.startCountdown(s.targetDurationMinutes);
      },
      error: () => {}
    });
  }

  stopSession(): void {
    if (!this.activeSession) return;
    this.http.post(`${this.base}/end/${this.activeSession.id}`, {}).subscribe({
      next: () => {
        this.activeSession = null;
        this.clearTimer();
        this.secondsLeft = 0;
        this.loadSummary();
      },
      error: () => {}
    });
  }

  private startCountdown(minutes: number): void {
    this.clearTimer();
    this.secondsLeft = minutes * 60;
    this.timer = setInterval(() => {
      if (this.secondsLeft > 0) this.secondsLeft--;
      else this.clearTimer();
    }, 1000);
  }

  private clearTimer(): void {
    if (this.timer) { clearInterval(this.timer); this.timer = null; }
  }

  get timerDisplay(): string {
    const m = Math.floor(this.secondsLeft / 60).toString().padStart(2, '0');
    const s = (this.secondsLeft % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }
}
