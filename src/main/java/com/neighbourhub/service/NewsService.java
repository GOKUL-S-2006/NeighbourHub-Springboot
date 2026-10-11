package com.neighbourhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.neighbourhub.model.Issue;
import com.neighbourhub.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE MMM dd yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter ISO_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final IssueRepository issues;
    private final RestClient http;
    private final String geminiKey;
    private final String geminiModel;

    public NewsService(IssueRepository issues, RestClient http,
                       @Value("${gemini.api-key:}") String geminiKey,
                       @Value("${gemini.model}") String geminiModel) {
        this.issues = issues;
        this.http = http;
        this.geminiKey = geminiKey;
        this.geminiModel = geminiModel;
    }

    public Map<String, Object> generateBrief() {
        ZonedDateTime now = ZonedDateTime.now();
        Instant today = now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
        Instant week = now.toLocalDate().minusDays(7).atStartOfDay(now.getZone()).toInstant();

        List<Issue> todayIssues = issues.findByCreatedAtGreaterThanEqual(today);
        List<Issue> weekIssues = issues.findByCreatedAtGreaterThanEqual(week);
        List<Issue> resolvedThisWeek = issues.findByStatusAndUpdatedAtGreaterThanEqual(Issue.RESOLVED, week);
        List<Issue> emergencyIssues = issues.findBySeverityAndStatusNot("Emergency", Issue.RESOLVED);
        List<Issue> topVoted = issues.findTop3ByOrderByVotesDesc();

        String date = now.format(DATE_FMT);
        String brief = null;
        String source = "fallback";

        String prompt = buildPrompt(date, todayIssues, weekIssues, resolvedThisWeek, emergencyIssues, topVoted);
        String aiText = callGemini(prompt);
        if (aiText != null) {
            brief = aiText;
            source = "gemini";
        } else {
            brief = fallbackBrief(todayIssues, resolvedThisWeek, emergencyIssues, topVoted, date);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayCount", todayIssues.size());
        stats.put("weekCount", weekIssues.size());
        stats.put("resolvedCount", resolvedThisWeek.size());
        stats.put("emergencyCount", emergencyIssues.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("brief", brief);
        out.put("source", source);
        out.put("generatedAt", ISO_MILLIS.format(now.toInstant()));
        out.put("stats", stats);
        return out;
    }

    /** Returns the generated text, or null if Gemini is unavailable (caller then uses the fallback). */
    private String callGemini(String prompt) {
        if (geminiKey == null || geminiKey.isBlank()) return null;
        try {
            Map<String, Object> body = Map.of("contents",
                    List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

            JsonNode res = http.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}",
                            geminiModel, geminiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (res == null || res.has("error")) return null;
            return res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception e) {
            log.warn("Gemini call failed, using fallback brief: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String date, List<Issue> today, List<Issue> week, List<Issue> resolved,
                               List<Issue> emergency, List<Issue> topVoted) {
        String dataSummary = "\nCommunity Data Summary for " + date + ":\n\n"
                + "NEW ISSUES TODAY (" + today.size() + "):\n"
                + orNone(lines(today, i -> "- " + i.getTitle() + " [" + i.getCategory() + ", " + i.getSeverity() + "]")) + "\n\n"
                + "ISSUES THIS WEEK (" + week.size() + " total):\n"
                + lines(week.stream().limit(5).toList(), i -> "- " + i.getTitle() + " [" + i.getStatus() + "]") + "\n\n"
                + "RESOLVED THIS WEEK (" + resolved.size() + "):\n"
                + orNone(lines(resolved, i -> "- " + i.getTitle() + " [" + i.getCategory() + "]")) + "\n\n"
                + "ACTIVE EMERGENCIES (" + emergency.size() + "):\n"
                + orNone(lines(emergency, i -> "- " + i.getTitle())) + "\n\n"
                + "TOP VOTED ISSUES:\n"
                + lines(topVoted, i -> "- " + i.getTitle() + " (" + i.getVotes() + " votes, " + i.getStatus() + ")") + "\n";

        return "\nYou are a neighborhood news reporter for a community app called NeighbourHub.\n"
                + "Based on the community data below, write a short, friendly daily news brief.\n\n"
                + "Rules:\n"
                + "- Start with \"NeighbourHub Daily Brief\" and today's date\n"
                + "- Use simple bullet points\n"
                + "- Maximum 8 bullet points\n"
                + "- Be informative and community-friendly\n"
                + "- Mention resolved issues positively\n"
                + "- Flag emergencies urgently\n"
                + "- End with one positive/motivational line for the community\n"
                + "- Do NOT use markdown, asterisks for bold, or hashtags\n"
                + "- Use plain text only with bullet points using \u2022\n\n"
                + dataSummary + "\n";
    }

    private String fallbackBrief(List<Issue> today, List<Issue> resolved, List<Issue> emergency,
                                 List<Issue> topVoted, String date) {
        StringBuilder b = new StringBuilder("NeighbourHub Daily Brief\n" + date + "\n\n");

        if (!emergency.isEmpty()) {
            b.append("\u2022 URGENT: ").append(emergency.size()).append(" emergency ")
                    .append(emergency.size() == 1 ? "issue requires" : "issues require")
                    .append(" immediate attention\n");
            emergency.stream().limit(2).forEach(i -> b.append("  - ").append(i.getTitle()).append("\n"));
        }

        if (!today.isEmpty()) {
            b.append("\u2022 ").append(today.size()).append(" new ")
                    .append(today.size() == 1 ? "issue was" : "issues were").append(" reported today\n");
            today.stream().limit(2).forEach(i -> b.append("  - ").append(i.getTitle()).append("\n"));
        } else {
            b.append("\u2022 No new issues reported today\n");
        }

        if (!resolved.isEmpty()) {
            b.append("\u2022 ").append(resolved.size()).append(" ")
                    .append(resolved.size() == 1 ? "issue was" : "issues were").append(" resolved this week\n");
            resolved.stream().limit(2).forEach(i -> b.append("  - ").append(i.getTitle()).append("\n"));
        }

        if (!topVoted.isEmpty()) {
            b.append("\u2022 Most discussed: ").append(topVoted.get(0).getTitle())
                    .append(" (").append(topVoted.get(0).getVotes()).append(" votes)\n");
        }

        b.append("\nKeep reporting issues to make our neighbourhood better!");
        return b.toString();
    }

    private static String lines(List<Issue> list, Function<Issue, String> f) {
        return list.stream().map(f).collect(Collectors.joining("\n"));
    }

    private static String orNone(String s) {
        return s.isEmpty() ? "None" : s;
    }
}
