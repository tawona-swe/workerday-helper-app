# Workday Helper

A full-stack productivity app for managing your daily tasks and reminders. Built with Angular on the frontend and Spring Boot on the backend, with JWT-based authentication so each user's data stays private.

## What it does

- Register and log in with a personal account
- Create, update, and delete tasks — filter by pending or completed
- Track task completion analytics
- Create and manage reminders with active/triggered states
- Update your profile name and password

---

## Tech Stack

| Layer    | Technology                              |
|----------|-----------------------------------------|
| Frontend | Angular 21, Tailwind CSS, Lucide icons  |
| Backend  | Spring Boot 3.1, Spring Security, JPA   |
| Database | H2 (file-based, persistent)             |
| Auth     | JWT (JJWT 0.11.5)                       |

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
