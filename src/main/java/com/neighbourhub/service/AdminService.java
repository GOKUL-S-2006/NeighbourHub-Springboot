package com.neighbourhub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighbourhub.exception.ApiException;
import com.neighbourhub.model.Issue;
import com.neighbourhub.model.User;
import com.neighbourhub.repository.IssueRepository;
import com.neighbourhub.repository.UserRepository;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final IssueRepository issues;
    private final UserRepository users;
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;

    public AdminService(IssueRepository issues, UserRepository users, MongoTemplate mongo, ObjectMapper mapper) {
        this.issues = issues;
        this.users = users;
        this.mongo = mongo;
        this.mapper = mapper;
    }

    public Map<String, Object> dashboardStats() {
        Document agg = mongo.aggregate(
                Aggregation.newAggregation(Aggregation.group().sum("votes").as("totalVotes")),
                "issues", Document.class).getUniqueMappedResult();
        long totalVotes = agg == null || agg.get("totalVotes") == null
                ? 0 : ((Number) agg.get("totalVotes")).longValue();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalIssues", issues.count());
        stats.put("openIssues", issues.countByStatus(Issue.OPEN));
        stats.put("inProgressIssues", issues.countByStatus(Issue.IN_PROGRESS));
        stats.put("resolvedIssues", issues.countByStatus(Issue.RESOLVED));
        stats.put("totalUsers", users.count());
        stats.put("totalVotes", totalVotes);
        return stats;
    }

    /** Paginated list; createdBy is expanded to { _id, name, email, role } like Mongoose populate. */
    public Map<String, Object> listIssues(int page, int limit, String status, String category) {
        Query query = new Query();
        if (status != null && !status.isEmpty()) query.addCriteria(Criteria.where("status").is(status));
        if (category != null && !category.isEmpty()) query.addCriteria(Criteria.where("category").is(category));

        long totalCount = mongo.count(query, Issue.class);

        query.with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .skip((long) (page - 1) * limit)
                .limit(limit);
        List<Issue> found = mongo.find(query, Issue.class);

        Set<String> creatorIds = found.stream()
                .filter(i -> i.getCreatedBy() != null)
                .map(i -> i.getCreatedBy().toHexString())
                .collect(Collectors.toSet());
        Map<String, User> creators = new HashMap<>();
        users.findAllById(creatorIds).forEach(u -> creators.put(u.getId(), u));

        List<Map<String, Object>> data = new ArrayList<>();
        for (Issue issue : found) {
            Map<String, Object> row = mapper.convertValue(issue, new TypeReference<LinkedHashMap<String, Object>>() {});
            if (issue.getCreatedBy() != null) {
                User u = creators.get(issue.getCreatedBy().toHexString());
                if (u == null) {
                    row.put("createdBy", null);
                } else {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("_id", u.getId());
                    summary.put("name", u.getName());
                    summary.put("email", u.getEmail());
                    summary.put("role", u.getRole());
                    row.put("createdBy", summary);
                }
            }
            data.add(row);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", page);
        body.put("limit", limit);
        body.put("totalPages", (int) Math.ceil((double) totalCount / limit));
        body.put("totalCount", totalCount);
        body.put("data", data);
        return body;
    }

    public Issue updateStatus(String id, String status) {
        if (status == null || !IssueService.STATUSES.contains(status)) {
            throw ApiException.message(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        Issue issue = issues.findById(id)
                .orElseThrow(() -> ApiException.message(HttpStatus.NOT_FOUND, "Issue not found"));
        issue.setStatus(status);
        return issues.save(issue);
    }

    public void deleteIssue(String id) {
        Issue issue = issues.findById(id)
                .orElseThrow(() -> ApiException.message(HttpStatus.NOT_FOUND, "Issue not found"));
        issues.delete(issue);
    }
}
