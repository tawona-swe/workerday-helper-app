# Workday Helper

An AI-powered productivity platform for managing your workday — tasks, reminders, focus sessions, health nudges, analytics, and an AI assistant, all in one place. Built with Angular on the frontend and Spring Boot on the backend, with JWT-based authentication so each user's data stays private.

## Live Deployment

| Layer    | URL                                                                 |
|----------|---------------------------------------------------------------------|
| Frontend | https://workerdayapp.tawonarwatida.co.zw                           |
| Backend  | https://workday.tawonarwatida.co.zw                                |
| Health   | https://workday.tawonarwatida.co.zw/api/health/ping                |

## Hosting Architecture (AWS)

```
User
 │
 ├── Frontend (Angular SPA)
 │    ├── Static files hosted on AWS S3
 │    ├── Served globally via AWS CloudFront CDN
 │    ├── HTTPS via SSL cert from AWS ACM (us-east-1)
 │    └── Custom domain via CNAME → CloudFront distribution
 │
 └── Backend (Spring Boot API)
      ├── Runs on AWS EC2 (Ubuntu) as a systemd service
      ├── Nginx reverse proxy handles SSL (Let's Encrypt)
      │    and forwards /api/* to Spring Boot on port 8080
      ├── Custom domain A record → EC2 public IP
      └── Connects to Groq LLM API for AI features
```

## What it does

- Register and log in with a personal account
- Create, update, and delete tasks with priorities and due dates
- AI assistant chat — ask questions, create tasks by conversation
- Smart task suggestions with AI-generated how-to-tackle advice
- Focus sessions with countdown timer linked to tasks
- Health reminders (eye breaks, hydration, posture, stretches)
- Productivity analytics — daily and weekly scores, peak focus window
- Gamification — points, streaks, and achievements
- Real-time notifications via SSE

---

## Tech Stack

| Layer    | Technology                                        |
|----------|---------------------------------------------------|
| Frontend | Angular 21, Tailwind CSS, Lucide icons            |
| Backend  | Spring Boot 3, Spring Security, JPA               |
| Database | H2 (file-based, persistent)                       |
| Auth     | JWT (JJWT)                                        |
| AI       | Groq API (llama-3.1-8b-instant)                   |
| Hosting  | AWS S3 + CloudFront (frontend), EC2 + Nginx (API) |

---

## Prerequisites

Make sure you have the following installed:

- Java 17+
- Maven 3.8+ (or use the included `mvnw` wrapper)
- Node.js 18+ and npm 11+
- Angular CLI: `npm install -g @angular/cli`

---

## Setup

### 1. Clone the repo

```bash
git clone https://github.com/tawona-swe/workerday-helper-app.git
cd workerday-helper-app
```

### 2. Backend

```bash
cd workday-helper-backend
./mvnw spring-boot:run
```

On Windows:
```bash
mvnw.cmd spring-boot:run
```

The API will start on `http://localhost:8080`.

The H2 database is file-based and stored in `workday-helper-backend/data/`. It's created automatically on first run.

You can access the H2 console at `http://localhost:8080/h2-console` with:
- JDBC URL: `jdbc:h2:file:./data/workdaydb`
- Username: `sa`
- Password: *(leave blank)*

> Before deploying to production, change `app.jwt.secret` in `application.properties` to a strong, unique 256-bit secret.

### 3. Frontend

```bash
cd workday-helper-frontend
npm install
ng serve
```

The app will be available at `http://localhost:4200`.

---

## API Overview

All endpoints under `/api/tasks` and `/api/reminders` require a `Bearer <token>` header.

| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | `/api/auth/register`              | Register a new user      |
| POST   | `/api/auth/login`                 | Login and get JWT token  |
| GET    | `/api/auth/me`                    | Get current user profile |
| PUT    | `/api/auth/me`                    | Update name/password     |
| GET    | `/api/tasks`                      | List all tasks           |
| GET    | `/api/tasks/pending`              | List pending tasks       |
| GET    | `/api/tasks/completed`            | List completed tasks     |
| POST   | `/api/tasks`                      | Create a task            |
| PUT    | `/api/tasks/{id}`                 | Update a task            |
| DELETE | `/api/tasks/{id}`                 | Delete a task            |
| GET    | `/api/tasks/analytics/completion-rate` | Task analytics      |
| GET    | `/api/reminders`                  | List all reminders       |
| GET    | `/api/reminders/active`           | List active reminders    |
| POST   | `/api/reminders`                  | Create a reminder        |
| PUT    | `/api/reminders/{id}`             | Update a reminder        |
| PATCH  | `/api/reminders/{id}/trigger`     | Mark reminder triggered  |
| DELETE | `/api/reminders/{id}`             | Delete a reminder        |

---

## Project Structure

```
workday-helper-backend/       # Spring Boot API
  src/main/java/com/workdayhelper/app/
    controller/               # REST controllers
    model/                    # JPA entities (User, Task, Reminder)
    repository/               # Spring Data repositories
    service/                  # Business logic
    security/                 # JWT filter, utils, UserDetailsService
    config/                   # Security and CORS config

workday-helper-frontend/      # Angular app
  src/app/
    components/               # Auth, dashboard, tasks, reminders
    services/                 # HTTP service layer
    models/                   # TypeScript interfaces
    auth/                     # Route guard and JWT interceptor
    layouts/                  # App shell and auth layout
```

## Feature Details

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

## Deploy (Frontend)

```bash
cd workday-helper-frontend
npm run deploy
```

Builds, uploads to S3, and invalidates CloudFront in one shot. Requires AWS CLI configured with appropriate permissions.
