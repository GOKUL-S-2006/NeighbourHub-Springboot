package com.neighbourhub.service;

import java.util.Locale;

/** Keyword-based category / severity detection (port of analyzeCaption in analyzeImage.js). */
public final class CaptionAnalyzer {
    private CaptionAnalyzer() {}

    public record Result(String category, String severity, String priority) {}

    public static Result analyze(String caption) {
        String text = caption == null ? "" : caption.toLowerCase(Locale.ROOT);

        String category = "general";
        if (containsAny(text, "road", "pothole", "crack", "pavement", "asphalt")) category = "road";
        else if (containsAny(text, "water", "flood", "drain", "pipe")) category = "water";
        else if (containsAny(text, "light", "electric", "wire", "power")) category = "electricity";
        else if (containsAny(text, "garbage", "waste", "trash", "litter")) category = "sanitation";

        String severity = "Low";
        if (containsAny(text, "large", "big", "severe", "broken", "flood", "danger", "collapsed")) severity = "High";
        else if (containsAny(text, "crack", "damage", "leak", "hole", "pothole")) severity = "Medium";

        String priority = switch (severity) {
            case "High" -> "high";
            case "Medium" -> "medium";
            default -> "low";
        };
        return new Result(category, severity, priority);
    }

    private static boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) return true;
        }
        return false;
    }
}
