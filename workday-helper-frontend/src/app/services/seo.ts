import { Injectable } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';

interface PageMeta {
  title: string;
  description: string;
}

const PAGE_META: Record<string, PageMeta> = {
  '/':            { title: 'WorkdayHelper — AI-Powered Productivity Platform', description: 'Manage your workday intelligently with AI chat, smart task suggestions, focus sessions, and health reminders.' },
  '/dashboard':   { title: 'Dashboard — WorkdayHelper', description: 'Your productivity overview: tasks, reminders, and weekly progress at a glance.' },
  '/tasks':       { title: 'My Tasks — WorkdayHelper', description: 'Create, manage, and complete your tasks with AI-powered prioritization.' },
  '/chat':        { title: 'AI Assistant — WorkdayHelper', description: 'Chat with your personal AI productivity coach to plan your day and tackle tasks.' },
  '/focus':       { title: 'Focus Sessions — WorkdayHelper', description: 'Start timed focus sessions linked to your tasks and track your deep work time.' },
  '/suggestions': { title: 'Smart Suggestions — WorkdayHelper', description: 'AI-ranked task suggestions with personalized advice on how to tackle each one.' },
  '/analytics':   { title: 'Analytics — WorkdayHelper', description: 'Track your productivity score, focus minutes, and peak performance windows.' },
  '/gamification':{ title: 'Rewards & Streaks — WorkdayHelper', description: 'Earn points, maintain streaks, and unlock achievements as you stay productive.' },
  '/reminders':   { title: 'Health Reminders — WorkdayHelper', description: 'Set eye break, posture, hydration, and custom reminders to stay healthy at work.' },
  '/login':       { title: 'Sign In — WorkdayHelper', description: 'Sign in to your WorkdayHelper account and get back to being productive.' },
  '/register':    { title: 'Create Account — WorkdayHelper', description: 'Join WorkdayHelper and start managing your workday with AI-powered tools.' },
};

@Injectable({ providedIn: 'root' })
export class SeoService {
  constructor(private titleService: Title, private metaService: Meta) {}

  update(path: string): void {
    const meta = PAGE_META[path] ?? PAGE_META['/'];
    this.titleService.setTitle(meta.title);
    this.metaService.updateTag({ name: 'description', content: meta.description });
    this.metaService.updateTag({ property: 'og:title', content: meta.title });
    this.metaService.updateTag({ property: 'og:description', content: meta.description });
    this.metaService.updateTag({ name: 'twitter:title', content: meta.title });
    this.metaService.updateTag({ name: 'twitter:description', content: meta.description });
  }
}
