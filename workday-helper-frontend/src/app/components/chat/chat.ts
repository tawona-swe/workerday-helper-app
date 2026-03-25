import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TaskService } from '../../services/task';
import { Task } from '../../models/task.model';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  createdAt?: string;
  taskCreated?: Task; // attached when a task was created from this message
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html'
})
export class ChatComponent implements OnInit {
  messages: ChatMessage[] = [];
  inputText = '';
  readonly maxLength = 2000;

  // Patterns that suggest the user wants to create a task
  private readonly taskTriggers = /\b(add|create|make|set up|schedule|remind me to|new task)\b/i;

  constructor(private http: HttpClient, private taskService: TaskService) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.http.get<ChatMessage[]>('http://localhost:8080/api/assistant/history').subscribe({
      next: msgs => this.messages = msgs,
      error: () => {}
    });
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text) return;
    this.messages.push({ role: 'user', content: text, createdAt: new Date().toISOString() });
    this.inputText = '';

    if (this.taskTriggers.test(text)) {
      this.handleTaskCreation(text);
    } else {
      this.sendToAssistant(text);
    }
  }

  private sendToAssistant(text: string): void {
    this.http.post<ChatMessage>('http://localhost:8080/api/assistant/message', { message: text }).subscribe({
      next: reply => this.messages.push({ ...reply, createdAt: reply.createdAt ?? new Date().toISOString() }),
      error: () => this.messages.push({ role: 'assistant', content: 'Error getting response.', createdAt: new Date().toISOString() })
    });
  }

  private handleTaskCreation(text: string): void {
    const task = this.parseTaskFromText(text);
    this.taskService.create(task).subscribe({
      next: created => {
        const due = created.dueDate ? ` due ${this.formatDate(created.dueDate)}` : '';
        this.messages.push({
          role: 'assistant',
          content: `Done! I've added "${created.title}" as a ${created.priority.toLowerCase()} priority task${due}.`,
          createdAt: new Date().toISOString(),
          taskCreated: created
        });
      },
      error: () => this.messages.push({
        role: 'assistant',
        content: `I couldn't create that task. Try again or head to the Tasks page.`,
        createdAt: new Date().toISOString()
      })
    });
  }

  private parseTaskFromText(text: string): Task {
    // Extract due date — looks for "by <date>", "on <date>", "for <date>"
    const dueDateMatch = text.match(/\b(?:by|on|for)\s+(\w+\s?\w*(?:\s+\d{1,2})?(?:,?\s*\d{4})?)/i);
    let dueDate: string | undefined;
    if (dueDateMatch) {
      const parsed = new Date(dueDateMatch[1]);
      if (!isNaN(parsed.getTime())) dueDate = parsed.toISOString().split('T')[0];
    }

    // Extract priority — looks for "high", "urgent", "low", "medium"
    let priority: Task['priority'] = 'MEDIUM';
    if (/\b(high|urgent|asap|important)\b/i.test(text)) priority = 'HIGH';
    else if (/\b(low|whenever|someday)\b/i.test(text)) priority = 'LOW';

    // Strip trigger words to get the task title
    const title = text
      .replace(/\b(add|create|make|set up|schedule|remind me to|new task|a task to|a task for)\b/gi, '')
      .replace(/\b(?:by|on|for)\s+\w+\s?\w*(?:\s+\d{1,2})?(?:,?\s*\d{4})?/gi, '')
      .replace(/\b(high|urgent|asap|important|low|whenever|someday|medium)\b/gi, '')
      .replace(/\s{2,}/g, ' ')
      .trim();

    return {
      title: title.charAt(0).toUpperCase() + title.slice(1),
      description: '',
      completed: false,
      priority,
      ...(dueDate && { dueDate })
    };
  }

  private formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric' });
  }

  formatTime(iso?: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  get charsRemaining(): number {
    return this.maxLength - this.inputText.length;
  }
}
