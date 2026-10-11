package com.neighbourhub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "issues") // same collection Mongoose used for model "Issue"
public class Issue {

    public static final String OPEN = "open";
    public static final String IN_PROGRESS = "in-progress";
    public static final String RESOLVED = "resolved";

    @Id
    @JsonProperty("_id")
    private String id;

    private String title;          // required
    private String description;
    private String category = "other";
    private String location;       // required, "lat,lon"
    private String image = "";
    private String status = OPEN;  // open | in-progress | resolved
    private int votes = 0;
    private List<ObjectId> votedBy = new ArrayList<>();
    private ObjectId createdBy;
    private String priority = "low"; // low | medium | high
    private String aiSummary;
    private String aiConfidence;
    private String severity = "Low"; // Low | Medium | High | Emergency

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
