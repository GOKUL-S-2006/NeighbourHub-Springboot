package com.neighbourhub.controller;

import com.neighbourhub.dto.Dtos.CreateIssueRequest;
import com.neighbourhub.dto.Dtos.StatusRequest;
import com.neighbourhub.dto.Dtos.UpdateIssueRequest;
import com.neighbourhub.model.Issue;
import com.neighbourhub.model.User;
import com.neighbourhub.service.IssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issues;

    public IssueController(IssueService issues) {
        this.issues = issues;
    }

    @PostMapping
    public ResponseEntity<Issue> create(@RequestBody CreateIssueRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issues.create(req, user));
    }

    @GetMapping
    public Map<String, List<Issue>> getAll() {
        return Map.of("data", issues.findAllByVotes());
    }

    @GetMapping("/my")
    public List<Issue> getMine(@AuthenticationPrincipal User user) {
        return issues.findMine(user);
    }

    @PutMapping("/{id}")
    public Issue update(@PathVariable String id, @RequestBody UpdateIssueRequest req,
                        @AuthenticationPrincipal User user) {
        return issues.update(id, req, user);
    }

    @PatchMapping("/{id}/status")
    public Issue updateStatus(@PathVariable String id, @RequestBody StatusRequest req) {
        return issues.updateStatus(id, req.status());
    }

    @PatchMapping("/{id}/upvote")
    public Issue upvote(@PathVariable String id, @AuthenticationPrincipal User user) {
        return issues.toggleUpvote(id, user);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable String id, @AuthenticationPrincipal User user) {
        issues.delete(id, user);
        return Map.of("message", "Issue deleted successfully \u2705");
    }
}
