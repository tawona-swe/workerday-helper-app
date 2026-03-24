# Requirements Document

## Introduction

The Smart Workday Assistant upgrades the existing Workday Helper application (Spring Boot + Angular) into an AI-powered productivity platform. The upgrade adds four priority capabilities: an AI chat assistant, smart task suggestions, an analytics dashboard, and a focus mode engine. Secondary capabilities include NLP task input, gamification, adaptive health reminders, and context-aware scheduling suggestions. All new features integrate with the existing Task, Reminder, and User models and are secured behind the existing JWT authentication layer.

## Glossary

- **Assistant**: The AI chat service that processes user messages and returns contextual responses using an LLM API (e.g., OpenAI).
- **NLP_Parser**: The backend component that parses natural-language task input into structured Task fields.
- **Task_Suggester**: The backend service that ranks and schedules tasks based on priority, due date, time of day, and user behavior history.
- **Focus_Engine**: The backend and frontend component that manages focus sessions, tracks deep work time, and enforces Pomodoro-style intervals.
- **Analytics_Engine**: The backend service that computes productivity scores, focus time, health adherence, and peak-hour detection from stored session and task data.
- **Gamification_Engine**: The backend service that tracks streaks, awards points, and unlocks achievements based on user activity.
- **Health_Monitor**: The backend component that tracks inactivity and session length to trigger adaptive break reminders.
- **Context_Engine**: The backend service that combines time of day, task list, and behavior history to generate scheduling suggestions.
- **Focus_Session**: A timed work interval started by the user, with a defined duration and an associated task.
- **Productivity_Score**: A numeric score (0–100) computed daily and weekly from task completion rate, focus time, and health adherence.
- **Streak**: A consecutive-day count of the user meeting a defined productivity threshold.
- **Chat_Message**: A single user or assistant turn in a conversation, stored with a timestamp and role (user/assistant).

---

## Requirements

### Requirement 1: AI Assistant Chat

**User Story:** As a user, I want to chat with an AI assistant about my workday, so that I can get personalized advice on planning, prioritization, and wellbeing.

#### Acceptance Criteria

1. WHEN a user sends a chat message, THE Assistant SHALL forward the message along with the user's current task list, time of day, and the last 10 Chat_Messages as context to the configured LLM API and return a response within 10 seconds.
2. WHEN the LLM API returns a response, THE Assistant SHALL persist both the user message and the assistant response as Chat_Messages associated with the authenticated user.
3. WHEN a user requests their chat history, THE Assistant SHALL return the most recent 50 Chat_Messages for that user in chronological order.
4. IF the LLM API returns an error or times out, THEN THE Assistant SHALL return a descriptive error message to the user and SHALL NOT persist the failed exchange.
5. IF a user sends a message exceeding 2000 characters, THEN THE Assistant SHALL reject the request with a 400 status and a descriptive error.
6. THE Assistant SHALL require a valid JWT token on all chat endpoints.
7. THE Assistant SHALL NOT expose the LLM API key in any client-facing response or log output.

---

### Requirement 2: NLP Task Input

**User Story:** As a user, I want to type tasks in natural language, so that I can create structured tasks without filling in multiple form fields.

#### Acceptance Criteria

1. WHEN a user submits a natural-language task string, THE NLP_Parser SHALL extract a task title, an optional due date/time, and an optional duration and return a structured Task object.
2. WHEN the NLP_Parser extracts a due date, THE NLP_Parser SHALL resolve relative expressions (e.g., "tomorrow", "next Monday") against the user's current local date.
3. IF the NLP_Parser cannot extract a title from the input, THEN THE NLP_Parser SHALL return a 422 status with a descriptive error indicating the input could not be parsed.
4. THE NLP_Parser SHALL parse the input string and return a structured Task object such that parsing the same input twice produces equivalent Task objects (idempotent parse).
5. WHEN a parsed Task is saved, THE Task_Suggester SHALL treat it identically to a manually created Task.

---

### Requirement 3: Smart Task Suggestions

**User Story:** As a user, I want the system to suggest an optimal task order and time blocks for my day, so that I can work on the right things at the right time without manual planning.

#### Acceptance Criteria

1. WHEN a user requests daily task suggestions, THE Task_Suggester SHALL return the user's pending tasks ranked by a composite score derived from priority, due date proximity, and estimated duration.
2. WHEN generating suggestions, THE Task_Suggester SHALL assign high-cognitive tasks (priority = HIGH) to time blocks between 08:00 and 12:00 local time and lighter tasks (priority = LOW) to time blocks after 14:00 local time.
3. WHEN a user has completed tasks on at least 3 prior days, THE Task_Suggester SHALL incorporate historical completion-time patterns into the ranking score.
4. IF a task has a due date within 24 hours, THEN THE Task_Suggester SHALL rank that task first regardless of other scoring factors.
5. THE Task_Suggester SHALL return suggestions within 2 seconds for a task list of up to 100 pending tasks.
6. WHEN a user requests suggestions, THE Task_Suggester SHALL include an estimated time block (start time and end time) for each suggested task based on estimated duration or a default of 30 minutes.

---

### Requirement 4: Focus Mode

**User Story:** As a user, I want to start a timed focus session linked to a task, so that I can track deep work time and receive Pomodoro-style break prompts.

#### Acceptance Criteria

1. WHEN a user starts a Focus_Session, THE Focus_Engine SHALL record the session start time, the associated task ID, and the target duration (default 25 minutes if not specified) and return a session ID.
2. WHEN a user ends a Focus_Session, THE Focus_Engine SHALL record the actual end time and compute the elapsed deep work minutes.
3. WHEN a Focus_Session reaches its target duration, THE Focus_Engine SHALL emit a break notification to the frontend with the message "Great work — time for a break."
4. WHEN a user requests their daily focus summary, THE Focus_Engine SHALL return the total deep work minutes, the number of completed Focus_Sessions, and a list of sessions with task titles for the current calendar day.
5. IF a user attempts to start a Focus_Session while another session is already active, THEN THE Focus_Engine SHALL reject the request with a 409 status and the ID of the active session.
6. IF a user ends a Focus_Session that does not belong to them, THEN THE Focus_Engine SHALL return a 403 status.
7. THE Focus_Engine SHALL persist all Focus_Sessions so that historical deep work data is available to the Analytics_Engine.

---

### Requirement 5: Analytics Dashboard

**User Story:** As a user, I want to see a dashboard of my productivity metrics, so that I can understand my work patterns and improve over time.

#### Acceptance Criteria

1. WHEN a user requests their daily analytics, THE Analytics_Engine SHALL return a Productivity_Score (0–100) computed as a weighted average of task completion rate (40%), total deep work minutes relative to a 4-hour target (40%), and health adherence rate (20%).
2. WHEN a user requests their weekly analytics, THE Analytics_Engine SHALL return Productivity_Scores for each of the last 7 calendar days and a 7-day rolling average.
3. WHEN a user has at least 5 days of Focus_Session data, THE Analytics_Engine SHALL identify the 2-hour window with the highest average deep work minutes and return it as the user's "peak productivity window."
4. WHEN a user requests analytics, THE Analytics_Engine SHALL return focus time (minutes), distraction time (minutes defined as logged-in time minus focus time), and health adherence score for the requested period.
5. THE Analytics_Engine SHALL return all analytics responses within 3 seconds.
6. IF a user has no data for a requested period, THEN THE Analytics_Engine SHALL return zeroed metrics rather than an error.

---

### Requirement 6: Smart Health Engine

**User Story:** As a user, I want the system to monitor my session activity and send adaptive break reminders, so that I avoid burnout and maintain physical wellbeing during long work sessions.

#### Acceptance Criteria

1. WHEN a Focus_Session has been active for 90 consecutive minutes without a break, THE Health_Monitor SHALL emit a break reminder notification with the message "You've been focused for 90 minutes — take a 5-minute break."
2. WHEN a user has been logged in and active for 60 minutes outside of a Focus_Session, THE Health_Monitor SHALL emit an activity reminder.
3. WHEN a user dismisses a break reminder, THE Health_Monitor SHALL suppress further reminders of the same type for 15 minutes.
4. IF the Health_Monitor cannot determine user activity state, THEN THE Health_Monitor SHALL default to inactive and SHALL NOT emit reminders.
5. THE Health_Monitor SHALL record each emitted reminder as a health event associated with the user for use in health adherence scoring by the Analytics_Engine.

---

### Requirement 7: Context-Aware Scheduling Suggestions

**User Story:** As a user, I want the system to suggest what to work on based on the time of day and my current energy patterns, so that I schedule demanding work when I am most effective.

#### Acceptance Criteria

1. WHEN a user requests context-aware suggestions, THE Context_Engine SHALL combine the current time of day, the user's pending task list, and the user's peak productivity window (if available) to return an ordered list of up to 5 task recommendations with a plain-language rationale for each.
2. WHEN the current time is within the user's detected peak productivity window, THE Context_Engine SHALL place HIGH priority tasks at the top of the recommendation list.
3. WHEN the current time is after 16:00 local time, THE Context_Engine SHALL include a wrap-up suggestion recommending the user review completed tasks and plan for the next day.
4. IF the user has no historical data, THEN THE Context_Engine SHALL apply default heuristics: HIGH priority tasks before 12:00, LOW priority tasks after 14:00, wrap-up after 16:00.

---

### Requirement 8: Gamification

**User Story:** As a user, I want to earn points, maintain streaks, and unlock achievements, so that I stay motivated to maintain productive habits.

#### Acceptance Criteria

1. WHEN a user completes a task, THE Gamification_Engine SHALL award 10 points to the user's total score.
2. WHEN a user completes a Focus_Session of at least 25 minutes, THE Gamification_Engine SHALL award 20 points to the user's total score.
3. WHEN a user meets the daily Productivity_Score threshold of 60 or above for consecutive calendar days, THE Gamification_Engine SHALL increment the user's Streak counter by 1 each day.
4. IF a user's Streak reaches 5, 10, or 30 days, THEN THE Gamification_Engine SHALL unlock the corresponding achievement and notify the user.
5. IF a user does not meet the daily Productivity_Score threshold on a given calendar day, THEN THE Gamification_Engine SHALL reset the user's Streak counter to 0.
6. WHEN a user requests their gamification profile, THE Gamification_Engine SHALL return the current point total, active Streak count, and list of unlocked achievements.
7. THE Gamification_Engine SHALL compute and persist all point and streak updates within 5 seconds of the triggering event.
