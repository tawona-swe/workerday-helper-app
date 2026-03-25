# Workday Helper

A productivity-focused Angular app that combines task management, smart reminders, focus sessions, and AI assistance to help you stay on top of your workday.

Built with Angular 21 · Connects to a Spring Boot backend at `http://localhost:8080`

---

## Features

### Dashboard
Overview of your day at a glance — pending and completed task counts, active reminders, completion rate, and a weekly task activity chart.

### Tasks
Full task management with create, edit, delete, and completion toggling. Each task supports a title, description, priority (LOW / MEDIUM / HIGH), and an optional due date. Tasks are split into pending and completed views with a high-priority counter.

### Reminders
Interval-based reminders that fire on a schedule while the app is open. Supports built-in types (Eye Break, Posture Check, Hydration, Stretch) and custom reminders. Triggers an in-app alert and a browser notification with an audio beep.

### Focus Sessions
Pomodoro-style focus timer. Start a session tied to a specific task, set a target duration, and track a live countdown. Keeps a summary of total sessions, total focus minutes, and average session length.

### AI Assistant (Chat)
Conversational AI assistant backed by the `/api/assistant` endpoint. Maintains full message history across sessions.

**Natural language task creation** — the chat detects task intent automatically. Just type naturally and the assistant will parse and create the task for you, then confirm with a priority badge.

Examples:
- `"add a task to review the report by Friday"`
- `"create a high priority task to call the client"`
- `"remind me to submit the invoice by March 30"`

Supports priority extraction (high / urgent / low) and due date parsing from phrases like `by Friday`, `on March 30`, `for next week`.

### AI Suggestions
AI-generated scheduling recommendations for your tasks. Provides:
- **Daily suggestions** — prioritised task schedule with suggested start/end times, rationale, and AI advice per task
- **Context suggestions** — context-aware recommendations based on your current workload

### Analytics
Productivity metrics pulled from your activity:
- Daily and weekly productivity scores
- Focus vs. distraction minutes
- Peak performance time window

### Gamification
Points, streaks, and achievements to keep you motivated. Tracks your current streak, longest streak, total points, and unlocked achievements with timestamps.

### Account & Auth
JWT-based authentication with login, registration, and password reset. Protected routes via an auth guard. Token is attached automatically via an HTTP interceptor.

---

## Getting Started

**Prerequisites:** Node.js, Angular CLI, and the backend running on port 8080.

```bash
npm install
ng serve
```

Navigate to `http://localhost:4200`.

## Build

```bash
ng build
```

Output goes to the `dist/` directory.

## Tests

```bash
ng test
```
