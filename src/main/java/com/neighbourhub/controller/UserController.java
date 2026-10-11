package com.neighbourhub.controller;

import com.neighbourhub.dto.Dtos.CreateUserRequest;
import com.neighbourhub.exception.ApiException;
import com.neighbourhub.model.User;
import com.neighbourhub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Admin-only in this port. In the Node app these two endpoints were public, which let anyone
 * create an admin account and list every user (including password hashes).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody CreateUserRequest req) {
        if (isBlank(req.name()) || isBlank(req.email()) || isBlank(req.password())) {
            throw ApiException.error(HttpStatus.BAD_REQUEST, "User validation failed: name, email and password are required");
        }
        if (req.password().length() < 6) {
            throw ApiException.error(HttpStatus.BAD_REQUEST,
                    "User validation failed: password: Path `password` is shorter than the minimum allowed length (6).");
        }
        String role = isBlank(req.role()) ? "user" : req.role();
        if (!Set.of("user", "admin").contains(role)) {
            throw ApiException.error(HttpStatus.BAD_REQUEST,
                    "User validation failed: role: `" + role + "` is not a valid enum value for path `role`.");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(req.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(encoder.encode(req.password()));
        user.setRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(users.save(user));
    }

    @GetMapping
    public List<User> getAll() {
        return users.findAll();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
