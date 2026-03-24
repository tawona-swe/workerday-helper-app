package com.workdayhelper.app.service;

import com.workdayhelper.app.model.ChatMessage;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.ChatMessageRepository;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssistantService {

    private final LlmClient llmClient;
    private final ChatMessageRepository chatMessageRepository;
    private final TaskRepository taskRepository;

    public AssistantService(LlmClient llmClient,
                            ChatMessageRepository chatMessageRepository,
                            TaskRepository taskRepository) {
        this.llmClient = llmClient;
        this.chatMessageRepository = chatMessageRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public ChatMessage sendMessage(User user, String text) {
        if (text != null && text.length() > 2000) {
            throw new IllegalArgumentException("Message exceeds 2000 characters");
        }

        // Fetch pending tasks for context
        List<Task> pendingTasks = taskRepository.findByUserAndCompleted(user, false);
        String taskTitles = pendingTasks.stream()
                .map(Task::getTitle)
                .collect(Collectors.joining(", "));

        // Build system prompt
        String systemPrompt = "You are a helpful productivity assistant. The user has the following pending tasks: "
                + (taskTitles.isEmpty() ? "none" : taskTitles)
                + ". Help them manage their workday effectively. Be concise and friendly.";

        // Fetch last 10 messages for context
        List<ChatMessage> allHistory = chatMessageRepository.findTop50ByUserOrderByCreatedAtAsc(user);
        List<ChatMessage> recentHistory = allHistory.size() > 10
                ? allHistory.subList(allHistory.size() - 10, allHistory.size())
                : allHistory;

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage msg : recentHistory) {
            Map<String, String> entry = new HashMap<>();
            entry.put("role", msg.getRole());
            entry.put("content", msg.getContent());
            messages.add(entry);
        }

        // Add current user message to the list before calling LLM
        Map<String, String> userEntry = new HashMap<>();
        userEntry.put("role", "user");
        userEntry.put("content", text);
        messages.add(userEntry);

        // Call LLM — if it throws LlmException, do NOT persist and rethrow
        String llmResponse = llmClient.chat(systemPrompt, messages);

        // Persist user message
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent(text);
        userMessage.setUser(user);
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(userMessage);

        // Persist assistant message
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(llmResponse);
        assistantMessage.setUser(user);
        assistantMessage.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(assistantMessage);

        return assistantMessage;
    }

    public List<ChatMessage> getHistory(User user) {
        return chatMessageRepository.findTop50ByUserOrderByCreatedAtAsc(user);
    }
}
