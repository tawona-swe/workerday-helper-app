package com.workdayhelper.app.service;

import com.workdayhelper.app.model.Task;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NlpParserService {

    // Duration patterns
    private static final Pattern DURATION_FOR_N_MINUTES = Pattern.compile(
            "\\bfor\\s+(\\d+)\\s*(?:minutes?|mins?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION_N_MINUTES = Pattern.compile(
            "\\b(\\d+)\\s*(?:minutes?|mins?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION_N_HOURS = Pattern.compile(
            "\\b(\\d+)\\s*hours?\\b", Pattern.CASE_INSENSITIVE);

    // Date patterns
    private static final Pattern DATE_TOMORROW = Pattern.compile(
            "\\btomorrow\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_NEXT_WEEKDAY = Pattern.compile(
            "\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_IN_N_DAYS = Pattern.compile(
            "\\bin\\s+(\\d+)\\s+days?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_ON_DATE = Pattern.compile(
            "\\bon\\s+(\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?)\\b", Pattern.CASE_INSENSITIVE);

    public Optional<Task> parse(String text, LocalDate userLocalDate) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String working = text;

        // Extract duration
        int durationMinutes = 30;
        boolean durationFound = false;

        Matcher m = DURATION_FOR_N_MINUTES.matcher(working);
        if (m.find()) {
            durationMinutes = Integer.parseInt(m.group(1));
            durationFound = true;
            working = working.substring(0, m.start()) + working.substring(m.end());
        }

        if (!durationFound) {
            m = DURATION_N_HOURS.matcher(working);
            if (m.find()) {
                durationMinutes = Integer.parseInt(m.group(1)) * 60;
                durationFound = true;
                working = working.substring(0, m.start()) + working.substring(m.end());
            }
        }

        if (!durationFound) {
            m = DURATION_N_MINUTES.matcher(working);
            if (m.find()) {
                durationMinutes = Integer.parseInt(m.group(1));
                working = working.substring(0, m.start()) + working.substring(m.end());
            }
        }

        // Extract due date
        LocalDateTime dueDate = null;

        m = DATE_TOMORROW.matcher(working);
        if (m.find()) {
            dueDate = userLocalDate.plusDays(1).atStartOfDay();
            working = working.substring(0, m.start()) + working.substring(m.end());
        }

        if (dueDate == null) {
            m = DATE_NEXT_WEEKDAY.matcher(working);
            if (m.find()) {
                DayOfWeek target = parseDayOfWeek(m.group(1));
                dueDate = nextOccurrence(userLocalDate, target).atStartOfDay();
                working = working.substring(0, m.start()) + working.substring(m.end());
            }
        }

        if (dueDate == null) {
            m = DATE_IN_N_DAYS.matcher(working);
            if (m.find()) {
                int days = Integer.parseInt(m.group(1));
                dueDate = userLocalDate.plusDays(days).atStartOfDay();
                working = working.substring(0, m.start()) + working.substring(m.end());
            }
        }

        if (dueDate == null) {
            m = DATE_ON_DATE.matcher(working);
            if (m.find()) {
                // Remove the "on [date]" phrase but don't parse the date further
                working = working.substring(0, m.start()) + working.substring(m.end());
            }
        }

        // Clean up title
        String title = working.replaceAll("\\s{2,}", " ").trim();
        if (title.isEmpty()) {
            // Fall back to original text stripped of extra whitespace
            title = text.replaceAll("\\s{2,}", " ").trim();
            if (title.isEmpty()) {
                return Optional.empty();
            }
        }

        Task task = new Task();
        task.setTitle(title);
        task.setEstimatedDurationMinutes(durationMinutes);
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }

        return Optional.of(task);
    }

    private DayOfWeek parseDayOfWeek(String name) {
        return switch (name.toLowerCase()) {
            case "monday"    -> DayOfWeek.MONDAY;
            case "tuesday"   -> DayOfWeek.TUESDAY;
            case "wednesday" -> DayOfWeek.WEDNESDAY;
            case "thursday"  -> DayOfWeek.THURSDAY;
            case "friday"    -> DayOfWeek.FRIDAY;
            case "saturday"  -> DayOfWeek.SATURDAY;
            case "sunday"    -> DayOfWeek.SUNDAY;
            default          -> throw new IllegalArgumentException("Unknown day: " + name);
        };
    }

    private LocalDate nextOccurrence(LocalDate from, DayOfWeek target) {
        LocalDate next = from.plusDays(1);
        while (next.getDayOfWeek() != target) {
            next = next.plusDays(1);
        }
        return next;
    }
}
