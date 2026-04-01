package com.workdayhelper.app.service;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.*;

@Service
public class CalendarService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public CalendarService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getAuthUrl(Long userId) {
        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(
            clientId,
            redirectUri,
            List.of(
                "https://www.googleapis.com/auth/calendar.events",
                "https://www.googleapis.com/auth/calendar.readonly"
            )
        );
        return url.setState(String.valueOf(userId))
                  .setAccessType("offline")
                  .setApprovalPrompt("force")
                  .build();
    }

    public void handleCallback(String code, Long userId) throws IOException, GeneralSecurityException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
            transport, JSON_FACTORY, clientId, clientSecret, code, redirectUri
        ).execute();

        User user = userRepository.findById(userId).orElseThrow();
        user.setGoogleAccessToken(tokenResponse.getAccessToken());
        if (tokenResponse.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokenResponse.getRefreshToken());
        }
        user.setGoogleCalendarConnected(true);
        userRepository.save(user);
    }

    public List<Map<String, Object>> getUpcomingEvents(User user) throws IOException, GeneralSecurityException {
        Calendar service = buildCalendarService(user);
        DateTime now = new DateTime(System.currentTimeMillis());
        DateTime weekAhead = new DateTime(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);

        Events events = service.events().list("primary")
            .setMaxResults(20)
            .setTimeMin(now)
            .setTimeMax(weekAhead)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Event event : events.getItems()) {
            Map<String, Object> e = new HashMap<>();
            e.put("id", event.getId());
            e.put("title", event.getSummary());
            e.put("start", event.getStart().getDateTime() != null
                ? event.getStart().getDateTime().toString()
                : event.getStart().getDate().toString());
            e.put("end", event.getEnd().getDateTime() != null
                ? event.getEnd().getDateTime().toString()
                : event.getEnd().getDate().toString());
            e.put("description", event.getDescription());
            result.add(e);
        }
        return result;
    }

    public String syncTask(User user, Task task) throws IOException, GeneralSecurityException {
        Calendar service = buildCalendarService(user);
        Event event = new Event().setSummary(task.getTitle());
        if (task.getDescription() != null) event.setDescription(task.getDescription());

        LocalDateTime due = task.getDueDate() != null
            ? task.getDueDate()
            : LocalDateTime.now().plusHours(1);

        EventDateTime start = new EventDateTime()
            .setDateTime(new DateTime(due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            .setTimeZone(ZoneId.systemDefault().getId());
        EventDateTime end = new EventDateTime()
            .setDateTime(new DateTime(due.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            .setTimeZone(ZoneId.systemDefault().getId());

        event.setStart(start).setEnd(end);
        Event created = service.events().insert("primary", event).execute();
        return created.getId();
    }

    private Calendar buildCalendarService(User user) throws IOException, GeneralSecurityException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = new GoogleCredential.Builder()
            .setTransport(transport)
            .setJsonFactory(JSON_FACTORY)
            .setClientSecrets(clientId, clientSecret)
            .build()
            .setAccessToken(user.getGoogleAccessToken())
            .setRefreshToken(user.getGoogleRefreshToken());

        // Refresh token and persist if changed
        boolean refreshed = credential.refreshToken();
        if (refreshed && !credential.getAccessToken().equals(user.getGoogleAccessToken())) {
            user.setGoogleAccessToken(credential.getAccessToken());
            userRepository.save(user);
        }

        return new Calendar.Builder(transport, JSON_FACTORY, credential)
            .setApplicationName("WorkdayHelper")
            .build();
    }
}
