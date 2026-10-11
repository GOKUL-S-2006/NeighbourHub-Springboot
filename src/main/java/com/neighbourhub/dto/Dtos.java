package com.neighbourhub.dto;

/** Request bodies. Missing JSON fields arrive as null, same as undefined in Express. */
public final class Dtos {
    private Dtos() {}

    public record SignupRequest(String name, String email, String password) {}

    public record LoginRequest(String email, String password) {}

    public record CreateIssueRequest(String title, String description, String category,
                                     String location, String image, String severity) {}

    public record UpdateIssueRequest(String title, String description, String category, String location) {}

    public record StatusRequest(String status) {}

    public record CreateUserRequest(String name, String email, String password, String role) {}
}
