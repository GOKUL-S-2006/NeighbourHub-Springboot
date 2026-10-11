package com.neighbourhub.exception;

import com.neighbourhub.model.Issue;

public class DuplicateIssueException extends RuntimeException {
    private final transient Issue existing;

    public DuplicateIssueException(Issue existing) {
        super("An issue has already been reported in this location.");
        this.existing = existing;
    }

    public Issue getExisting() { return existing; }
}
