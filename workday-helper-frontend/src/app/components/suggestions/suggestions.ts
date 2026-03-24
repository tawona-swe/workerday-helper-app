import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface TaskSuggestion {
  taskId: number;
  title: string;
  suggestedStartTime: string;
  suggestedEndTime: string;
  rationale: string;
}

interface ContextSuggestion {
  taskId: number;
  title: string;
  suggestedStartTime: string;
  suggestedEndTime: string;
  rationale: string;
}

@Component({
  selector: 'app-suggestions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './suggestions.html'
})
export class SuggestionsComponent implements OnInit {
  private readonly base = 'http://localhost:8080/api/suggestions';

  daily: TaskSuggestion[] = [];
  context: ContextSuggestion[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<TaskSuggestion[]>(`${this.base}/daily`).subscribe({
      next: d => this.daily = d,
      error: () => {}
    });
    this.http.get<ContextSuggestion[]>(`${this.base}/context`).subscribe({
      next: c => this.context = c,
      error: () => {}
    });
  }
}
