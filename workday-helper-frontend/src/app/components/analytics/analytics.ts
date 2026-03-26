import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface DailyAnalytics {
  productivityScore: number;
  focusMinutes: number;
  distractionMinutes: number;
  date?: string;
}

interface WeeklyAnalytics {
  productivityScore: number;
  focusMinutes: number;
  distractionMinutes: number;
  weekStart?: string;
}

interface TimeWindow {
  startTime: string;
  endTime: string;
  label?: string;
}

import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analytics.html'
})
export class AnalyticsDashboardComponent implements OnInit {
  private readonly base = `${environment.apiUrl}/api/analytics`;

  daily: DailyAnalytics | null = null;
  weekly: WeeklyAnalytics | null = null;
  peakWindow: TimeWindow | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<DailyAnalytics>(`${this.base}/daily`).subscribe({ next: d => this.daily = d, error: () => {} });
    this.http.get<WeeklyAnalytics>(`${this.base}/weekly`).subscribe({ next: w => this.weekly = w, error: () => {} });
    this.http.get<TimeWindow>(`${this.base}/peak-window`).subscribe({ next: p => this.peakWindow = p, error: () => {} });
  }

  scoreWidth(score: number): string {
    return `${Math.min(100, Math.max(0, score))}%`;
  }
}
