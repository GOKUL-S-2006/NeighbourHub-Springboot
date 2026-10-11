package com.neighbourhub.controller;

import com.neighbourhub.dto.Dtos.StatusRequest;
import com.neighbourhub.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Access is restricted to role ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return admin.dashboardStats();
    }

    @GetMapping("/issues")
    public Map<String, Object> issues(@RequestParam(required = false) String page,
                                      @RequestParam(required = false) String limit,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String category) {
        return admin.listIssues(positiveOr(page, 1), positiveOr(limit, 10), status, category);
    }

    @PatchMapping("/issues/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable String id, @RequestBody StatusRequest req) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "Status updated successfully");
        out.put("issue", admin.updateStatus(id, req.status()));
        return out;
    }

    @DeleteMapping("/issues/{id}")
    public Map<String, String> delete(@PathVariable String id) {
        admin.deleteIssue(id);
        return Map.of("message", "Issue deleted by admin successfully \u2705");
    }

    /** Mirrors parseInt(x) || default, but also rejects zero / negative values. */
    private static int positiveOr(String value, int fallback) {
        try {
            int n = Integer.parseInt(value.trim());
            return n > 0 ? n : fallback;
        } catch (RuntimeException e) { // null or not a number
            return fallback;
        }
    }
}
