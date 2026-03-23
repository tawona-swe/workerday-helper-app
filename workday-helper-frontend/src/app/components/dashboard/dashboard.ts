import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, Eye, PersonStanding, Droplets, Dumbbell, Bell, Pencil, Check, LucideIconData } from 'lucide-angular';
import { TaskService } from '../../services/task';
import { ReminderService } from '../../services/reminder';
import { Task } from '../../models/task.model';
import { Reminder } from '../../models/reminder.model';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit {
  tasks: Task[] = [];
  reminders: Reminder[] = [];
  analytics: any = {};

  readonly Eye = Eye;
  readonly PersonStanding = PersonStanding;
  readonly Droplets = Droplets;
  readonly Dumbbell = Dumbbell;
  readonly Bell = Bell;
  readonly Pencil = Pencil;
  readonly Check = Check;

  typeIconMap: Record<string, LucideIconData> = {
    EYE_BREAK: Eye, POSTURE: PersonStanding, HYDRATION: Droplets, STRETCH: Dumbbell, CUSTOM: Bell
  };

  weekDays: { label: string; height: number }[] = [];

  constructor(private taskService: TaskService, private reminderService: ReminderService) {}

  ngOnInit(): void {
    this.taskService.getAll().subscribe(t => {
      this.tasks = t;
      this.weekDays = this.buildWeekChart(t);
    });
    this.reminderService.getAll().subscribe(r => this.reminders = r);
    this.taskService.getAnalytics().subscribe(a => this.analytics = a);
  }

  private buildWeekChart(tasks: Task[]): { label: string; height: number }[] {
    const labels = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
    // Get Monday of current week
    const now = new Date();
    const dayOfWeek = now.getDay(); // 0=Sun
    const monday = new Date(now);
    monday.setDate(now.getDate() - ((dayOfWeek + 6) % 7));
    monday.setHours(0, 0, 0, 0);

    // Count tasks created each day Mon–Sun this week
    const counts = [0, 0, 0, 0, 0, 0, 0];
    for (const task of tasks) {
      if (!task.createdAt) continue;
      const d = new Date(task.createdAt);
      const diff = Math.floor((d.getTime() - monday.getTime()) / 86400000);
      if (diff >= 0 && diff < 7) counts[diff]++;
    }

    // Scale to max bar height of 80px, min 6px
    const max = Math.max(...counts, 1);
    return labels.map((label, i) => ({
      label,
      height: Math.round((counts[i] / max) * 74) + 6
    }));
  }

  get pendingCount(): number { return this.tasks.filter(t => !t.completed).length; }
  get completedCount(): number { return this.tasks.filter(t => t.completed).length; }
  get activeReminders(): number { return this.reminders.filter(r => r.active).length; }
  get pending(): Task[] { return this.tasks.filter(t => !t.completed); }
  get completionRate(): number {
    if (!this.tasks.length) return 0;
    return Math.round((this.completedCount / this.tasks.length) * 100);
  }
}
