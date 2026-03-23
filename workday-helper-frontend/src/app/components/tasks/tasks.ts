import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Pencil, Trash2, Calendar, CheckCircle2, Circle } from 'lucide-angular';
import { TaskService } from '../../services/task';
import { Task } from '../../models/task.model';

@Component({
  selector: 'app-tasks',
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './tasks.html',
  styleUrl: './tasks.scss'
})
export class TasksComponent implements OnInit {
  tasks: Task[] = [];
  showForm = false;
  editingTask: Task | null = null;

  readonly Pencil = Pencil;
  readonly Trash2 = Trash2;
  readonly Calendar = Calendar;
  readonly CheckCircle2 = CheckCircle2;
  readonly Circle = Circle;

  newTask: Task = { title: '', description: '', completed: false, priority: 'MEDIUM' };

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.taskService.getAll().subscribe(t => this.tasks = t);
  }

  save(): void {
    if (!this.newTask.title.trim()) return;
    if (this.editingTask) {
      this.taskService.update({ ...this.editingTask, ...this.newTask }).subscribe(() => {
        this.load(); this.cancel();
      });
    } else {
      this.taskService.create(this.newTask).subscribe(() => {
        this.load(); this.cancel();
      });
    }
  }

  edit(task: Task): void {
    this.editingTask = task;
    this.newTask = { ...task };
    this.showForm = true;
  }

  toggle(task: Task): void {
    this.taskService.update({ ...task, completed: !task.completed }).subscribe(() => this.load());
  }

  delete(id: number): void {
    this.taskService.delete(id).subscribe(() => this.load());
  }

  cancel(): void {
    this.showForm = false;
    this.editingTask = null;
    this.newTask = { title: '', description: '', completed: false, priority: 'MEDIUM' };
  }

  get pending(): Task[] { return this.tasks.filter(t => !t.completed); }
  get completed(): Task[] { return this.tasks.filter(t => t.completed); }
  get highPriorityCount(): number { return this.tasks.filter(t => t.priority === 'HIGH' && !t.completed).length; }
}
