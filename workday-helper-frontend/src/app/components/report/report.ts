import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface DayScore { day: string; score: number; focusMinutes: number; }
interface AiInsights { summary: string; wentWell: string[]; improve: string[]; nextWeekTip: string; }
interface WeeklyReport {
  weekStart: string; weekEnd: string;
  tasksCreated: number; tasksCompleted: number; tasksPending: number; highPriorityTasks: number;
  totalFocusMinutes: number; avgProductivityScore: number; bestDay: string;
  dailyScores: DayScore[];
  ai: AiInsights;
}

@Component({
  selector: 'app-report',
  imports: [CommonModule],
  templateUrl: './report.html'
})
export class ReportComponent implements OnInit {
  report: WeeklyReport | null = null;
  loading = true;
  error = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<WeeklyReport>(`${environment.apiUrl}/api/report/weekly`).subscribe({
      next: r => { this.report = r; this.loading = false; },
      error: () => { this.error = 'Failed to load report.'; this.loading = false; }
    });
  }

  refresh(): void { this.report = null; this.load(); }

  barHeight(score: number): number { return Math.round((score / 100) * 64) + 4; }

  formatHours(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  }

  completionRate(): number {
    if (!this.report || this.report.tasksCreated === 0) return 0;
    return Math.round((this.report.tasksCompleted / this.report.tasksCreated) * 100);
  }
}
