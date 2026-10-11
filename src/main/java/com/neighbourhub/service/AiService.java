package com.neighbourhub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final String ANALYZE_PROMPT =
            "You are an assistant for a neighbourhood issue reporting app. Analyze this image and respond ONLY in this exact JSON format, no extra text:\n"
            + "{\n  \"title\": \"Short 4-6 word title of the issue\",\n"
            + "  \"description\": \"One clear sentence describing the damage or problem visible\"\n}";

    private final GroqClient groq;
    private final ObjectMapper mapper;

    public AiService(GroqClient groq, ObjectMapper mapper) {
        this.groq = groq;
        this.mapper = mapper;
    }

    // ===================== POST /api/analyze-image =====================
    public Map<String, Object> analyzeImage(MultipartFile image) throws IOException {
        String raw = groq.chat(List.of(
                GroqClient.imagePart(dataUrl(image)),
                GroqClient.textPart(ANALYZE_PROMPT)), 150);
        log.info("Raw AI response: {}", raw);

        String title = "Neighbourhood Issue";
        String description = "Issue detected. Please add details.";
        try {
            JsonNode parsed = mapper.readTree(stripFences(raw));
            if (!parsed.path("title").asText("").isEmpty()) title = parsed.get("title").asText();
            if (!parsed.path("description").asText("").isEmpty()) description = parsed.get("description").asText();
        } catch (JsonProcessingException e) {
            description = raw; // model replied with plain text
        }

        CaptionAnalyzer.Result r = CaptionAnalyzer.analyze(description);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("description", description);
        out.put("category", r.category());
        out.put("severity", r.severity());
        out.put("priority", r.priority());
        return out;
    }

    // ===================== POST /api/verify-repair =====================
    public Map<String, Object> verifyRepair(MultipartFile afterImage, String beforeImageUrl,
                                            String category, String issueTitle) throws IOException {
        boolean hasBefore = beforeImageUrl != null && !beforeImageUrl.isBlank();

        String prompt = "You are an AI verifier for a government accountability system.\n"
                + (hasBefore ? "Image 1 is the BEFORE photo showing the reported issue."
                             : "There is no before photo available.") + "\n"
                + "The last image is the AFTER photo uploaded by the government admin claiming the issue is fixed.\n\n"
                + "Issue title: \"" + issueTitle + "\"\n"
                + "Category: \"" + category + "\"\n\n"
                + "Analyze carefully and respond ONLY in this exact JSON format, no extra text:\n"
                + "{\n  \"isRepaired\": true or false,\n  \"confidence\": a number from 0 to 100,\n"
                + "  \"reason\": \"One clear sentence explaining your decision\"\n}\n\n"
                + "Be strict \u2014 only confirm repair if there is clear visible evidence of the fix.";

        List<Map<String, Object>> parts = new ArrayList<>();
        if (hasBefore) parts.add(GroqClient.imagePart(beforeImageUrl));
        parts.add(GroqClient.imagePart(dataUrl(afterImage)));
        parts.add(GroqClient.textPart(prompt));

        String raw = groq.chat(parts, 200);
        log.info("Verify AI response: {}", raw);

        boolean isRepaired = false;
        Object confidence = 0;
        String reason = "Could not verify repair.";
        try {
            JsonNode parsed = mapper.readTree(stripFences(raw));
            isRepaired = parsed.path("isRepaired").isBoolean() && parsed.get("isRepaired").asBoolean();

            JsonNode c = parsed.get("confidence");
            if (c != null && c.isNumber() && c.asDouble() != 0) confidence = c.numberValue();
            else if (c != null && c.isTextual() && !c.asText().isEmpty()) confidence = c.asText();

            if (!parsed.path("reason").asText("").isEmpty()) reason = parsed.get("reason").asText();
        } catch (JsonProcessingException e) {
            reason = raw;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("isRepaired", isRepaired);
        out.put("confidence", confidence);
        out.put("reason", reason);
        return out;
    }

    // ===================== helpers =====================
    private static String dataUrl(MultipartFile file) throws IOException {
        String mime = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
    }

    /** Models sometimes wrap JSON in ```json fences; strip them before parsing. */
    private static String stripFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return s;
    }
}
