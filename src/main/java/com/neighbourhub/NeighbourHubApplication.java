package com.neighbourhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration is excluded: we authenticate with our own JWT filter,
// so Spring's default generated-password user is not wanted.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class NeighbourHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(NeighbourHubApplication.class, args);
    }
}
