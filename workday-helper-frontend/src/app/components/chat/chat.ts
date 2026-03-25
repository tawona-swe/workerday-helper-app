import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  createdAt?: string;
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

  constructor(private http: HttpClient) {}

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
    this.http.post<ChatMessage>('http://localhost:8080/api/assistant/message', { message: text }).subscribe({
      next: reply => this.messages.push({ ...reply, createdAt: reply.createdAt ?? new Date().toISOString() }),
      error: () => this.messages.push({ role: 'assistant', content: 'Error getting response.', createdAt: new Date().toISOString() })
    });
  }

  formatTime(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  get charsRemaining(): number {
    return this.maxLength - this.inputText.length;
  }
}
