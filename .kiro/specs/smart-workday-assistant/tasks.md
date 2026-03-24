# Implementation Plan: Smart Workday Assistant

## Overview

Incrementally extend the existing Spring Boot + Angular Workday Helper into an AI-powered productivity platform. Each task builds on the previous, wiring new components into the existing JWT-secured architecture. The implementation follows the layered order: infrastructure → data layer → services → controllers → frontend → integration.

## Tasks

- [x] 1. Add dependencies and infrastructure configuration
  - Add jqwik 1.8.1 test dependency to `pom.xml`
  - Add `app.llm.api-key` and `app.llm.base-url` properties to `application.properties`
  - Configure a `RestTemplate` bean with 10-second read timeout for LLM HTTP calls
  - Create `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping `LlmException` → 503, `ConflictException` → 409, `ForbiddenException` → 403, `EntityNotFoundException` → 404, `ConstraintViolationException` → 400, unhandled → 500
  - Create custom exception classes: `LlmException`, `ConflictException`, `ForbiddenException`
  - _Requirements: 1.4, 4.5, 4.6_

- [x] 2. Extend Task entity and add new JPA entities
  - [x] 2.1 Add `estimatedDurationMinutes` field (default 30) to the existing `Task` entity
    - Update getter/setter; no migration needed (`ddl-auto=update`)
    - _Requirements: 3.6_

  - [x] 2.2 Create `ChatMessage` entity and `ChatMessageRepository`
    - Fields: `id`, `role` (user/assistant), `content` (length 2000), `createdAt`, `@ManyToOne User`
    - Repository method: `findTop50ByUserOrderByCreatedAtAsc(user)`
    - _Requirements: 1.2, 1.3_

  - [x] 2.3 Create `FocusSession` entity and `FocusSessionRepository`
    - Fields: `id`, `startTime`, `endTime` (nullable), `targetDurationMinutes`, `actualDurationMinutes`, `completed`, `@ManyToOne Task`, `@ManyToOne User`
    - Repository methods: `findByUserAndEndTimeIsNull(user)`, `findByUserAndStartTimeBetween(user, start, end)`
    - _Requirements: 4.1, 4.7_

  - [x] 2.4 Create `GamificationProfile`, `Achievement` entities and their repositories
    - `GamificationProfile`: `id`, `totalPoints`, `currentStreak`, `longestStreak`, `lastStreakDate`, `@OneToMany achievements`, `@OneToOne User`
    - `Achievement`: `id`, `name`, `description`, `unlockedAt`, `@ManyToOne GamificationProfile`
    - Repository methods: `GamificationProfileRepository.findByUser(user)`, `AchievementRepository.findByProfile(profile)`
    - _Requirements: 8.1, 8.3, 8.4, 8.6_

  - [x] 2.5 Create `HealthEvent` entity and `HealthEventRepository`
    - Fields: `id`, `type` (BREAK_90MIN / ACTIVITY_60MIN), `emittedAt`, `dismissed`, `dismissedAt`, `@ManyToOne User`
    - Repository method: `findByUserAndEmittedAtBetween(user, start, end)`
    - _Requirements: 6.5_

- [x] 3. Implement LlmClient
  - [x] 3.1 Create `LlmClient` wrapper class
    - Inject the configured `RestTemplate` and read `app.llm.api-key` / `app.llm.base-url` from properties
    - Implement `chat(systemPrompt, messages) -> String`; on timeout or HTTP error throw `LlmException`
    - Never include the API key in any return value or log statement
    - _Requirements: 1.1, 1.4, 1.7_

  - [ ]* 3.2 Write property test for API key absence from responses (Property 6)
    - **Property 6: API key absent from response**
    - **Validates: Requirements 1.7**

- [x] 4. Implement AssistantService and AssistantController
  - [x] 4.1 Implement `AssistantService`
    - `sendMessage(user, text)`: validate length ≤ 2000, build context (task list + last 10 messages), call `LlmClient.chat`, persist both turns; on `LlmException` do not persist and rethrow
    - `getHistory(user)`: delegate to `ChatMessageRepository.findTop50ByUserOrderByCreatedAtAsc`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ]* 4.2 Write property tests for AssistantService (Properties 1, 2, 3)
    - **Property 1: Chat message persistence round-trip**
    - **Property 2: Chat history ordering and limit**
    - **Property 3: LLM error does not persist messages**
    - **Validates: Requirements 1.2, 1.3, 1.4**

  - [x] 4.3 Implement `AssistantController` at `/api/assistant`
    - `POST /api/assistant/message` — validate body length, delegate to `AssistantService.sendMessage`, return 400 for oversized input
    - `GET /api/assistant/history` — delegate to `AssistantService.getHistory`
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.6_

  - [ ]* 4.4 Write property tests for AssistantController (Properties 4, 5)
    - **Property 4: Oversized message rejected with 400**
    - **Property 5: Unauthenticated requests rejected with 401**
    - **Validates: Requirements 1.5, 1.6**

- [x] 5. Checkpoint — Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement NlpParserService and NlpController
  - [x] 6.1 Implement `NlpParserService`
    - `parse(text, userLocalDate) -> Optional<Task>`: extract title (required), optional due date resolving relative expressions against `userLocalDate`, optional duration; return empty if no title found
    - Parsing must be deterministic/idempotent for the same input + reference date
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ]* 6.2 Write property tests for NlpParserService (Properties 7, 8)
    - **Property 7: Parse idempotence**
    - **Property 8: Relative date resolution**
    - **Validates: Requirements 2.4, 2.1, 2.2**

  - [x] 6.3 Implement `NlpController` at `/api/nlp`
    - `POST /api/nlp/parse` — call `NlpParserService.parse`; map empty result to HTTP 422; on success save via `TaskService.create` and return the created Task
    - _Requirements: 2.1, 2.3, 2.5_

  - [ ]* 6.4 Write property test for NlpController (Property 9)
    - **Property 9: Invalid input returns 422**
    - **Validates: Requirements 2.3**

- [x] 7. Implement TaskSuggesterService and SuggestionController
  - [x] 7.1 Implement `TaskSuggesterService`
    - `getDailySuggestions(user, localTime) -> List<TaskSuggestion>`: compute composite score from priority, due date proximity, estimated duration, and historical completion patterns (when ≥ 3 days of data); apply due-within-24h override (rank first); assign time blocks (HIGH → 08:00–12:00, LOW → 14:00+); return sorted list
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [ ]* 7.2 Write property tests for TaskSuggesterService (Properties 10, 11, 12, 13, 14)
    - **Property 10: Parsed and manual tasks treated equally by suggester**
    - **Property 11: Suggestions ranked by composite score**
    - **Property 12: Time block assignment by priority**
    - **Property 13: Due-within-24h task ranks first**
    - **Property 14: Each suggestion has a non-null time block with endTime after startTime**
    - **Validates: Requirements 2.5, 3.1, 3.2, 3.4, 3.6**

  - [x] 7.3 Implement `SuggestionController` at `/api/suggestions`
    - `GET /api/suggestions/daily` — delegate to `TaskSuggesterService.getDailySuggestions`
    - _Requirements: 3.1, 3.5_

- [x] 8. Implement FocusEngineService and FocusController
  - [x] 8.1 Implement `FocusEngineService`
    - `startSession(user, taskId, durationMinutes)`: check for active session via `findByUserAndEndTimeIsNull`; throw `ConflictException` if found; persist new `FocusSession` with `startTime=now`, `targetDurationMinutes` (default 25)
    - `endSession(user, sessionId)`: verify ownership (throw `ForbiddenException` if mismatch); set `endTime=now`, compute `actualDurationMinutes` via `ChronoUnit.MINUTES.between`, set `completed=true`; trigger `GamificationEngineService.onFocusSessionCompleted` if duration ≥ 25
    - `getDailySummary(user, date)`: aggregate sessions for the calendar day
    - `getActiveSession(user)`: delegate to repository
    - _Requirements: 4.1, 4.2, 4.4, 4.5, 4.6, 4.7_

  - [ ]* 8.2 Write property tests for FocusEngineService (Properties 15, 16, 17, 18, 19)
    - **Property 15: Session start fields correct and persisted**
    - **Property 16: Focus session duration computation**
    - **Property 17: Duplicate session start rejected with 409**
    - **Property 18: Cross-user session end rejected with 403**
    - **Property 19: Daily focus summary totals correct**
    - **Validates: Requirements 4.1, 4.2, 4.4, 4.5, 4.6, 4.7**

  - [x] 8.3 Implement `FocusController` at `/api/focus`
    - `POST /api/focus/start` — start session, return session DTO
    - `POST /api/focus/end/{sessionId}` — end session, return updated session DTO
    - `GET /api/focus/summary` — return daily summary for today
    - `GET /api/focus/active` — return active session or 204
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 9. Checkpoint — Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement AnalyticsEngineService and AnalyticsController
  - [x] 10.1 Implement `AnalyticsEngineService`
    - `getDailyAnalytics(user, date)`: compute `productivityScore = round(completionRate*40 + min(focusMinutes/240.0,1.0)*40 + healthAdherenceRate*20)` clamped to [0,100]; compute `distractionMinutes = loggedInMinutes - focusMinutes` (never negative); return zeroed DTO if no data
    - `getWeeklyAnalytics(user)`: return 7 daily scores + arithmetic mean rounded to 2 decimal places
    - `getPeakWindow(user)`: find 2-hour slot with highest total focus minutes across ≥ 5 days of data
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 10.2 Write property tests for AnalyticsEngineService (Properties 20, 21, 22, 23)
    - **Property 20: Productivity score formula**
    - **Property 21: Weekly rolling average**
    - **Property 22: Peak window detection**
    - **Property 23: Distraction time equals logged-in minus focus, never negative**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

  - [x] 10.3 Implement `AnalyticsController` at `/api/analytics`
    - `GET /api/analytics/daily` — daily analytics for today (or `?date=` param)
    - `GET /api/analytics/weekly` — weekly analytics
    - `GET /api/analytics/peak-window` — peak productivity window
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 11. Implement GamificationEngineService and GamificationController
  - [x] 11.1 Implement `GamificationEngineService`
    - `onTaskCompleted(user)`: award 10 points; create profile if absent
    - `onFocusSessionCompleted(user, durationMinutes)`: award 20 points if duration ≥ 25
    - `onDailyScoreRecorded(user, score)`: if score ≥ 60 increment streak (update `longestStreak` if exceeded), else reset to 0; unlock achievement at milestones 5, 10, 30 (exactly once each); persist within 5 seconds
    - `getProfile(user)`: return profile with achievements
    - Wire `onTaskCompleted` call into `TaskService.update` when `completed` transitions to true
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [ ]* 11.2 Write property tests for GamificationEngineService (Properties 31, 32, 33, 34)
    - **Property 31: Points accumulation for tasks and sessions**
    - **Property 32: Streak correctness — increment and reset**
    - **Property 33: Achievement unlock at streak milestones**
    - **Property 34: Gamification profile completeness**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6**

  - [x] 11.3 Implement `GamificationController` at `/api/gamification`
    - `GET /api/gamification/profile` — return full profile DTO
    - _Requirements: 8.6_

- [x] 12. Implement HealthMonitorService and HealthController
  - [x] 12.1 Implement `HealthMonitorService`
    - `recordActivity(user)`: update last-activity timestamp in memory or a lightweight store
    - `dismissReminder(user, type)`: record dismissal time; suppress same-type reminders for 15 minutes
    - `getHealthEvents(user, date)`: delegate to `HealthEventRepository`
    - Scheduled check (`@Scheduled`): emit `BREAK_90MIN` if active session ≥ 90 min; emit `ACTIVITY_60MIN` if active outside session ≥ 60 min; skip if activity state unknown; persist each emitted event via `HealthEventRepository`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 12.2 Write property tests for HealthMonitorService (Properties 24, 25, 26, 27)
    - **Property 24: Reminder emitted after activity threshold**
    - **Property 25: Dismissed reminder suppressed for 15 minutes**
    - **Property 26: Unknown activity state emits no reminders**
    - **Property 27: Health events persisted on emit**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5**

  - [x] 12.3 Implement `HealthController` at `/api/health`
    - `POST /api/health/activity` — call `recordActivity`
    - `POST /api/health/dismiss/{type}` — call `dismissReminder`
    - `GET /api/health/events` — return today's health events
    - _Requirements: 6.3, 6.5_

- [x] 13. Implement ContextEngineService and SSE notification system
  - [x] 13.1 Implement `ContextEngineService`
    - `getSuggestions(user, localTime)`: combine pending tasks, peak window (if available), and time-of-day heuristics; return ≤ 5 `ContextSuggestion` items each with non-null rationale; apply wrap-up suggestion at or after 16:00; default heuristics when no history
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 13.2 Write property tests for ContextEngineService (Properties 28, 29, 30)
    - **Property 28: Context suggestions max 5 with rationale**
    - **Property 29: Peak hours ordering**
    - **Property 30: Wrap-up suggestion after 16:00**
    - **Validates: Requirements 7.1, 7.2, 7.3**

  - [x] 13.3 Implement `SuggestionController` context endpoint
    - `GET /api/suggestions/context` — delegate to `ContextEngineService.getSuggestions`
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 13.4 Implement `NotificationController` SSE endpoint at `/api/notifications/stream`
    - Return `SseEmitter`; register emitter per authenticated user; push break and achievement events from `HealthMonitorService` and `GamificationEngineService`
    - _Requirements: 4.3, 8.4_

- [x] 14. Checkpoint — Ensure all tests pass, ask the user if questions arise.

- [x] 15. Implement Angular frontend components
  - [x] 15.1 Create `NotificationService` (shared Angular service)
    - Subscribe to `/api/notifications/stream` via `EventSource`; display toast on receipt
    - _Requirements: 4.3, 8.4_

  - [ ]* 15.2 Write Angular unit test for NotificationService
    - Verify toast is displayed when SSE event is received
    - _Requirements: 4.3_

  - [x] 15.3 Create `ChatComponent` at route `/chat`
    - Message input with 2000-char limit, scrollable history list, send button; call `AssistantService` HTTP methods
    - _Requirements: 1.1, 1.3, 1.5_

  - [ ]* 15.4 Write Angular unit test for ChatComponent
    - Test message send, history display, and character limit enforcement
    - _Requirements: 1.1, 1.5_

  - [x] 15.5 Create `FocusComponent` at route `/focus`
    - Start/stop session controls, countdown timer display, active session status; call `FocusController` endpoints
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ]* 15.6 Write Angular unit test for FocusComponent
    - Test timer display and session state transitions
    - _Requirements: 4.1, 4.2_

  - [x] 15.7 Create `SuggestionsComponent` at route `/suggestions`
    - Display daily suggestions list and context-aware suggestions; show time blocks and rationale
    - _Requirements: 3.1, 3.6, 7.1_

  - [x] 15.8 Create `AnalyticsDashboardComponent` at route `/analytics`
    - Display daily/weekly productivity scores, peak window, focus vs distraction time
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 15.9 Write Angular unit test for AnalyticsDashboardComponent
    - Test score display and zero-data state rendering
    - _Requirements: 5.1, 5.6_

  - [x] 15.10 Create `GamificationComponent` at route `/gamification`
    - Display points, streak counter, and achievements list
    - _Requirements: 8.6_

  - [x] 15.11 Add new routes to Angular router and navigation links
    - Register `/chat`, `/focus`, `/suggestions`, `/analytics`, `/gamification` routes
    - _Requirements: 1.1, 3.1, 4.1, 5.1, 8.6_

  - [ ]* 15.12 Write Angular HTTP interceptor test
    - Verify JWT is attached to all outbound requests
    - _Requirements: 1.6_

- [x] 16. Final checkpoint — Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik with `@Property(tries = 100)` and must include the comment `// Feature: smart-workday-assistant, Property N: <property_text>`
- The `LlmClient` must be mocked in all unit and property tests to avoid real API calls
- `GamificationEngineService.onTaskCompleted` must be wired into `TaskService` when task completion transitions to true
