package com.workdayhelper.app.service;

import com.workdayhelper.app.exception.LlmException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmClient {

    private final RestTemplate restTemplate;

    @Value("${app.llm.api-key}")
    private String apiKey;

    @Value("${app.llm.base-url}")
    private String baseUrl;

    @Value("${app.llm.model:Claude-Sonnet-4-5}")
    private String model;

    public LlmClient(@Qualifier("llmRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(String systemPrompt, List<Map<String, String>> messages) {
        List<Map<String, String>> allMessages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        allMessages.add(systemMessage);
        allMessages.addAll(messages);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", allMessages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = baseUrl + "/chat/completions";
            org.slf4j.LoggerFactory.getLogger(LlmClient.class).info("LLM request to: {}, model: {}", url, model);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    url,
                    entity,
                    Map.class
            );

            if (response == null) {
                throw new LlmException("Received null response from LLM API");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LlmException("LLM API returned no choices in response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                throw new LlmException("LLM API response missing message content");
            }

            return (String) message.get("content");

        } catch (RestClientException e) {
            throw new LlmException("Failed to communicate with LLM API: " + e.getMessage(), e);
        }
    }
}
