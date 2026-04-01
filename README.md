# Workday Helper

An AI-powered productivity platform for managing your workday — tasks, reminders, focus sessions, health nudges, Google Calendar integration, AI day planning, weekly reports, and an AI assistant, all in one place. Built with Angular on the frontend and Spring Boot on the backend, with JWT-based authentication so each user's data stays private.

## Live Deployment

| Layer    | URL                                                                 |
|----------|---------------------------------------------------------------------|
| Frontend | https://workerdayapp.tawonarwatida.co.zw                           |
| Backend  | https://workday.tawonarwatida.co.zw                                |
| Health   | https://workday.tawonarwatida.co.zw/api/health/ping                |
| Privacy  | https://workerdayapp.tawonarwatida.co.zw/privacy                   |
| Terms    | https://workerdayapp.tawonarwatida.co.zw/terms                     |

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
- Smart task suggestions with AI-generated how-to-tackle advice (calendar-aware)
- Focus sessions with countdown timer linked to tasks
- Health reminders (eye breaks, hydration, posture, stretches)
- Productivity analytics — daily and weekly scores, peak focus window
- Gamification — points, streaks, and achievements
- Real-time notifications via SSE
- Google Calendar integration — view events on dashboard, sync tasks to calendar, conflict detection in suggestions
- AI Day Planner — generates an optimal daily schedule from tasks and calendar events, with approve mechanism to update task due dates
- Weekly AI Report — AI-generated summary of the week with productivity scores, task breakdown, what went well, and tips for next week

---

## Tech Stack

| Layer    | Technology                                        |
|----------|---------------------------------------------------|
| Frontend | Angular 21, Tailwind CSS, Lucide icons            |
| Backend  | Spring Boot 3, Spring Security, JPA               |
| Database | H2 (file-based, persistent)                       |
| Auth     | JWT (JJWT)                                        |
| AI       | Groq API (llama-3.1-8b-instant)                   |
| Calendar | Google Calendar API (OAuth 2.0)                   |
| Hosting  | AWS S3 + CloudFront (frontend), EC2 + Nginx (API) |

---

## Prerequisites

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

### 2. Backend environment

Create `workday-helper-backend/.env`:

```env
JWT_SECRET=your-256-bit-secret
JWT_EXPIRATION_MS=86400000
LLM_API_KEY=your-groq-api-key
LLM_BASE_URL=https://api.groq.com/openai/v1
LLM_MODEL=llama-3.1-8b-instant
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/calendar/callback
```

### 3. Run the backend

```bash
cd workday-helper-backend
./mvnw spring-boot:run
```

On Windows:
```bash
mvnw.cmd spring-boot:run
```

API starts on `http://localhost:8080`. H2 console available at `http://localhost:8080/h2-console`.

### 4. Run the frontend

```bash
cd workday-helper-frontend
npm install
ng serve
```

App available at `http://localhost:4200`.

---

## API Overview

All endpoints except `/api/auth/**`, `/api/health/ping`, and `/api/calendar/callback` require a `Bearer <token>` header.

| Method | Endpoint                               | Description                        |
|--------|----------------------------------------|------------------------------------|
| POST   | `/api/auth/register`                   | Register a new user                |
| POST   | `/api/auth/login`                      | Login and get JWT token            |
| GET    | `/api/auth/me`                         | Get current user profile           |
| PUT    | `/api/auth/me`                         | Update name/password               |
| GET    | `/api/tasks`                           | List all tasks                     |
| POST   | `/api/tasks`                           | Create a task                      |
| PUT    | `/api/tasks/{id}`                      | Update a task                      |
| DELETE | `/api/tasks/{id}`                      | Delete a task                      |
| GET    | `/api/reminders`                       | List all reminders                 |
| POST   | `/api/reminders`                       | Create a reminder                  |
| GET    | `/api/focus/active`                    | Get active focus session           |
| POST   | `/api/focus/start`                     | Start a focus session              |
| POST   | `/api/focus/end/{id}`                  | End a focus session                |
| GET    | `/api/analytics/daily`                 | Daily analytics                    |
| GET    | `/api/analytics/weekly`                | Weekly analytics                   |
| GET    | `/api/suggestions/daily`               | AI daily task suggestions          |
| GET    | `/api/suggestions/context`             | Context-aware suggestions          |
| POST   | `/api/assistant/message`               | Send message to AI assistant       |
| GET    | `/api/assistant/history`               | Get chat history                   |
| GET    | `/api/calendar/auth-url`               | Get Google OAuth URL               |
| GET    | `/api/calendar/callback`               | Google OAuth callback              |
| GET    | `/api/calendar/events`                 | Get upcoming calendar events       |
| POST   | `/api/calendar/sync-task/{id}`         | Sync task to Google Calendar       |
| GET    | `/api/calendar/status`                 | Check calendar connection status   |
| DELETE | `/api/calendar/disconnect`             | Disconnect Google Calendar         |
| GET    | `/api/planner/plan`                    | Generate AI day plan               |
| GET    | `/api/report/weekly`                   | Generate weekly AI report          |
| GET    | `/api/health/ping`                     | Health check (public)              |

---

## Project Structure

```
workday-helper-backend/
  src/main/java/com/workdayhelper/app/
    controller/       # REST controllers (11 controllers)
    model/            # JPA entities
    repository/       # Spring Data repositories
    service/          # Business logic + AI services
    security/         # JWT filter, utils, UserDetailsService
    config/           # Security, CORS, app config
    dto/              # Data transfer objects

workday-helper-frontend/
  src/app/
    components/       # All page components
      auth/           # Login, register, account
      dashboard/      # Main dashboard with calendar widget
      tasks/          # Task management
      reminders/      # Health reminders
      focus/          # Focus sessions
      chat/           # AI assistant
      suggestions/    # AI suggestions (calendar-aware)
      analytics/      # Productivity analytics
      gamification/   # Points, streaks, achievements
      calendar/       # Google Calendar integration
      planner/        # AI Day Planner
      report/         # Weekly AI Report
      landing/        # Public landing page
      privacy/        # Privacy policy
      terms/          # Terms of service
    services/         # HTTP service layer
    models/           # TypeScript interfaces
    auth/             # Route guard + JWT interceptor
    layouts/          # App shell and auth layout
```

## Deploy (Frontend)

```bash
cd workday-helper-frontend
npm run deploy
```

Builds, uploads to S3, and invalidates CloudFront in one command. Requires AWS CLI configured with S3 and CloudFront permissions.
