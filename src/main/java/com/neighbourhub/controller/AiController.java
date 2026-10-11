package com.neighbourhub.controller;

import com.neighbourhub.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiService ai;

    public AiController(AiService ai) {
        this.ai = ai;
    }

    @PostMapping("/analyze-image")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestPart(value = "image", required = false) MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No image provided"));
        }
        try {
            return ResponseEntity.ok(ai.analyzeImage(image));
        } catch (Exception e) {
            log.error("Analysis error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to analyze image"));
        }
    }

    @PostMapping("/verify-repair")
    public ResponseEntity<Map<String, Object>> verifyRepair(
            @RequestPart(value = "afterImage", required = false) MultipartFile afterImage,
            @RequestParam(required = false) String beforeImageUrl,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String issueTitle) {
        if (afterImage == null || afterImage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "After image is required"));
        }
        try {
            return ResponseEntity.ok(ai.verifyRepair(afterImage, beforeImageUrl, category, issueTitle));
        } catch (Exception e) {
            log.error("Verify repair error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to verify repair"));
        }
    }
}
