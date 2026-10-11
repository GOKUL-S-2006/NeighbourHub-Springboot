package com.neighbourhub.service;

import com.neighbourhub.dto.Dtos.CreateIssueRequest;
import com.neighbourhub.dto.Dtos.UpdateIssueRequest;
import com.neighbourhub.exception.ApiException;
import com.neighbourhub.exception.DuplicateIssueException;
import com.neighbourhub.model.Issue;
import com.neighbourhub.model.User;
import com.neighbourhub.repository.IssueRepository;
import com.neighbourhub.util.GeoUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class IssueService {

    public static final Set<String> STATUSES = Set.of(Issue.OPEN, Issue.IN_PROGRESS, Issue.RESOLVED);
    private static final Set<String> SEVERITIES = Set.of("Low", "Medium", "High", "Emergency");
    private static final double DUPLICATE_RADIUS_METERS = 100;

    private final IssueRepository issues;

    public IssueService(IssueRepository issues) {
        this.issues = issues;
    }

    // ===================== CREATE =====================
    public Issue create(CreateIssueRequest req, User user) {
        checkForDuplicate(req);

        List<String> errors = new ArrayList<>();
        if (isBlank(req.title())) errors.add("title: Path `title` is required.");
        if (isBlank(req.location())) errors.add("location: Path `location` is required.");

        String severity = isBlank(req.severity()) ? "Low" : req.severity();
        if (!SEVERITIES.contains(severity)) {
            errors.add("severity: `" + severity + "` is not a valid enum value for path `severity`.");
        }
        if (!errors.isEmpty()) {
            throw ApiException.error(HttpStatus.BAD_REQUEST, "Issue validation failed: " + String.join(", ", errors));
        }

        Issue issue = new Issue();
        issue.setTitle(req.title());
        issue.setDescription(req.description());
        issue.setCategory(req.category() != null ? req.category() : "other");
        issue.setLocation(req.location());
        issue.setImage(req.image() != null ? req.image() : "");
        issue.setSeverity(severity);
        issue.setCreatedBy(new ObjectId(user.getId()));
        return issues.save(issue);
    }

    /** Same category + within 100m of an open / in-progress issue => duplicate (409). */
    private void checkForDuplicate(CreateIssueRequest req) {
        if (isBlank(req.location())) return;
        double[] here = GeoUtils.parseLatLon(req.location());
        if (here == null) return;

        for (Issue existing : issues.findByStatusIn(List.of(Issue.OPEN, Issue.IN_PROGRESS))) {
            double[] there = GeoUtils.parseLatLon(existing.getLocation());
            if (there == null) continue;

            double distance = GeoUtils.distanceMeters(here[0], here[1], there[0], there[1]);
            if (distance <= DUPLICATE_RADIUS_METERS && Objects.equals(existing.getCategory(), req.category())) {
                throw new DuplicateIssueException(existing);
            }
        }
    }

    // ===================== READ =====================
    public List<Issue> findAllByVotes() {
        return issues.findAll(Sort.by(Sort.Direction.DESC, "votes"));
    }

    public List<Issue> findMine(User user) {
        return issues.findByCreatedByOrderByCreatedAtDesc(new ObjectId(user.getId()));
    }

    // ===================== UPVOTE (toggle) =====================
    public Issue toggleUpvote(String id, User user) {
        Issue issue = get(id);
        ObjectId userId = new ObjectId(user.getId());

        if (issue.getVotedBy().contains(userId)) {
            issue.setVotes(Math.max(0, issue.getVotes() - 1));
            issue.getVotedBy().remove(userId);
        } else {
            issue.setVotes(issue.getVotes() + 1);
            issue.getVotedBy().add(userId);
        }
        return issues.save(issue);
    }

    // ===================== UPDATE (owner only) =====================
    public Issue update(String id, UpdateIssueRequest req, User user) {
        Issue issue = get(id);
        if (!isOwner(issue, user)) {
            throw ApiException.message(HttpStatus.FORBIDDEN, "Not authorized");
        }
        if (req.title() != null) issue.setTitle(req.title());
        if (req.description() != null) issue.setDescription(req.description());
        if (req.category() != null) issue.setCategory(req.category());
        if (req.location() != null) issue.setLocation(req.location());
        return issues.save(issue);
    }

    // ===================== STATUS =====================
    public Issue updateStatus(String id, String status) {
        if (status == null || !STATUSES.contains(status)) {
            throw ApiException.error(HttpStatus.BAD_REQUEST,
                    "`" + status + "` is not a valid enum value for path `status`.");
        }
        Issue issue = get(id);
        issue.setStatus(status);
        return issues.save(issue);
    }

    // ===================== DELETE (owner or admin) =====================
    public void delete(String id, User user) {
        Issue issue = get(id);
        if (!isOwner(issue, user) && !"admin".equals(user.getRole())) {
            throw ApiException.message(HttpStatus.FORBIDDEN, "Not authorized");
        }
        issues.delete(issue);
    }

    // ===================== helpers =====================
    public Issue get(String id) {
        return issues.findById(id)
                .orElseThrow(() -> ApiException.message(HttpStatus.NOT_FOUND, "Issue not found"));
    }

    private static boolean isOwner(Issue issue, User user) {
        return issue.getCreatedBy() != null && issue.getCreatedBy().toHexString().equals(user.getId());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
