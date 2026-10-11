package com.neighbourhub.repository;

import com.neighbourhub.model.Issue;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface IssueRepository extends MongoRepository<Issue, String> {

    List<Issue> findByStatusIn(Collection<String> statuses);

    List<Issue> findByCreatedByOrderByCreatedAtDesc(ObjectId createdBy);

    long countByStatus(String status);

    // news brief queries
    List<Issue> findByCreatedAtGreaterThanEqual(Instant since);

    List<Issue> findByStatusAndUpdatedAtGreaterThanEqual(String status, Instant since);

    List<Issue> findBySeverityAndStatusNot(String severity, String status);

    List<Issue> findTop3ByOrderByVotesDesc();
}
