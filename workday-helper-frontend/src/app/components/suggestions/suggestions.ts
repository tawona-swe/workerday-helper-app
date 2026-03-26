import { Component, OnInit } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface TaskSuggestion {
  taskId: number;
  title: string;
  priority: string;
  suggestedStartTime: string;
  suggestedEndTime: string;
  rationale: string;
  aiAdvice?: string;
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

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    let done = 0;
    const finish = () => { if (++done === 2) this.loading = false; };

    this.http.get<TaskSuggestion[]>(`${this.base}/daily`).subscribe({
      next: d => { this.daily = d; finish(); },
      error: () => finish()
    });
    this.http.get<ContextSuggestion[]>(`${this.base}/context`).subscribe({
      next: c => { this.context = c; finish(); },
      error: () => finish()
    });
  }

  priorityColor(priority: string): string {
    if (priority === 'HIGH') return 'bg-red-100 text-red-700';
    if (priority === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-gray-100 text-gray-500';
  }
}
