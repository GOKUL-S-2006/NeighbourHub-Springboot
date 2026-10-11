package com.neighbourhub.security;

import com.neighbourhub.model.User;
import com.neighbourhub.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Port of middleware/auth.js. Reads "Authorization: Bearer token", verifies it, loads the
 * user from MongoDB and puts it in the security context. It never rejects by itself: public
 * endpoints must keep working, and protected ones are rejected by SecurityConfig using the
 * message stored in the AUTH_ERROR request attribute.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR = "authError";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String userId = jwtService.extractUserId(token);
                Optional<User> user = userId == null ? Optional.empty() : userRepository.findById(userId);

                if (user.isPresent()) {
                    User u = user.get();
                    var auth = new UsernamePasswordAuthenticationToken(
                            u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().toUpperCase())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    request.setAttribute(AUTH_ERROR, "User not found");
                }
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(AUTH_ERROR, "Invalid token");
            }
        }

        chain.doFilter(request, response);
    }
}
