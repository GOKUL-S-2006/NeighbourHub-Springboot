package com.neighbourhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Minimal client for Groq's OpenAI-compatible chat endpoint (replaces the groq-sdk package). */
@Service
public class GroqClient {

    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestClient http;
    private final String apiKey;
    private final String model;

    public GroqClient(RestClient http,
                      @Value("${groq.api-key:}") String apiKey,
                      @Value("${groq.model}") String model) {
        this.http = http;
        this.apiKey = apiKey;
        this.model = model;
    }

    public static Map<String, Object> imagePart(String url) {
        return Map.of("type", "image_url", "image_url", Map.of("url", url));
    }

    public static Map<String, Object> textPart(String text) {
        return Map.of("type", "text", "text", text);
    }

    /** Sends one user message made of image/text parts. Returns the trimmed reply, or "{}" if empty. */
    public String chat(List<Map<String, Object>> content, int maxTokens) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(Map.of("role", "user", "content", content)));

        JsonNode res = http.post()
                .uri(URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String text = res == null ? "" : res.path("choices").path(0).path("message").path("content").asText("").trim();
        return text.isEmpty() ? "{}" : text;
    }
}
