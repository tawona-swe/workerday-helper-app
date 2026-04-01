import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TaskCountPipe, BreakCountPipe, HighPriorityCountPipe } from './planner.pipes';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

interface PlanBlock {
  time: string;
  endTime: string;
  title: string;
  type: 'task' | 'break' | 'meeting';
  priority: string | null;
  taskId: number | null;
  tip: string;
}

@Component({
  selector: 'app-planner',
  imports: [CommonModule, TaskCountPipe, BreakCountPipe, HighPriorityCountPipe],
  templateUrl: './planner.html'
})
export class PlannerComponent {
  plan: PlanBlock[] = [];
  loading = false;
  generated = false;
  approved = false;
  approving = false;
  error = '';

  constructor(private http: HttpClient) {}

  generate(): void {
    this.loading = true;
    this.error = '';
    this.approved = false;
    this.http.get<PlanBlock[]>(`${environment.apiUrl}/api/planner/plan`).subscribe({
      next: p => { this.plan = p; this.loading = false; this.generated = true; },
      error: e => { this.error = e.error?.message || 'Failed to generate plan.'; this.loading = false; }
    });
  }

  regenerate(): void {
    this.generated = false;
    this.approved = false;
    this.plan = [];
    this.generate();
  }

  approvePlan(): void {
    this.approving = true;
    const today = new Date();
    const taskBlocks = this.plan.filter(b => b.type === 'task');

    const requests = taskBlocks.map(block => {
      const [h, m] = block.time.split(':').map(Number);
      const dueDate = new Date(today);
      dueDate.setHours(h, m, 0, 0);
      const dueDateStr = dueDate.toISOString().slice(0, 16); // datetime-local format

      if (block.taskId) {
        // Update existing task's due date
        return this.http.put(`${environment.apiUrl}/api/tasks/${block.taskId}`, {
          dueDate: dueDateStr
        }).pipe(catchError(() => of(null)));
      } else {
        // Create new task from plan block
        return this.http.post(`${environment.apiUrl}/api/tasks`, {
          title: block.title,
          priority: block.priority || 'MEDIUM',
          dueDate: dueDateStr,
          estimatedDurationMinutes: this.durationMinutes(block)
        }).pipe(catchError(() => of(null)));
      }
    });

    if (requests.length === 0) {
      this.approved = true;
      this.approving = false;
      return;
    }

    forkJoin(requests).subscribe(() => {
      this.approved = true;
      this.approving = false;
    });
  }

  blockColor(block: PlanBlock): string {
    if (block.type === 'break') return 'bg-emerald-50 border-emerald-200';
    if (block.type === 'meeting') return 'bg-blue-50 border-blue-200';
    if (block.priority === 'HIGH') return 'bg-red-50 border-red-200';
    if (block.priority === 'MEDIUM') return 'bg-yellow-50 border-yellow-200';
    return 'bg-white/70 border-white/80';
  }

  blockIcon(block: PlanBlock): string {
    if (block.type === 'break') return '☕';
    if (block.type === 'meeting') return '📅';
    if (block.priority === 'HIGH') return '🔴';
    if (block.priority === 'MEDIUM') return '🟡';
    return '🟢';
  }

  durationMinutes(block: PlanBlock): number {
    const [sh, sm] = block.time.split(':').map(Number);
    const [eh, em] = block.endTime.split(':').map(Number);
    return (eh * 60 + em) - (sh * 60 + sm);
  }
}
