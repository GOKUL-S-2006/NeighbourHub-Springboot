package com.neighbourhub.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** POST /api/upload  (multipart field "image") -> uploads to Cloudinary, returns Cloudinary's JSON. */
@RestController
@RequestMapping("/api")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final Cloudinary cloudinary;
    private final String folder;

    public UploadController(Cloudinary cloudinary, @Value("${cloudinary.folder}") String folder) {
        this.cloudinary = cloudinary;
        this.folder = folder;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestPart(value = "image", required = false) MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No file uploaded"));
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.asMap("folder", folder));
            return ResponseEntity.ok(result); // includes secure_url, public_id, ...
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Server error"));
        }
    }
}
