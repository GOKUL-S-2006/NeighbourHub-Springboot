package com.neighbourhub.service;

import com.neighbourhub.dto.Dtos.LoginRequest;
import com.neighbourhub.dto.Dtos.SignupRequest;
import com.neighbourhub.exception.ApiException;
import com.neighbourhub.model.User;
import com.neighbourhub.repository.UserRepository;
import com.neighbourhub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public void signup(SignupRequest req) {
        String name = trim(req.name());
        String email = trim(req.email());
        String password = trim(req.password());

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            throw ApiException.message(HttpStatus.BAD_REQUEST, "All fields are required");
        }
        email = email.toLowerCase(Locale.ROOT);

        if (users.findByEmail(email).isPresent()) {
            throw ApiException.message(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        users.save(user);
    }

    /** Returns a signed JWT, or throws 400 "Invalid credentials". */
    public String login(LoginRequest req) {
        String email = trim(req.email()).toLowerCase(Locale.ROOT);
        String password = trim(req.password());

        User user = users.findByEmail(email)
                .orElseThrow(() -> ApiException.message(HttpStatus.BAD_REQUEST, "Invalid credentials"));

        if (!encoder.matches(password, user.getPassword())) {
            throw ApiException.message(HttpStatus.BAD_REQUEST, "Invalid credentials");
        }
        return jwt.generateToken(user);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
