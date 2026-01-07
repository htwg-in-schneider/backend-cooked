package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public User me(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("https://cooked.api/email");
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("No email claim in token. Add 'email' or custom claim.");
        }

        User u = userRepository.findFirstByEmailIgnoreCase(email.trim());
        if (u == null) {
            User created = new User();
            created.setEmail(email.trim());
            created.setName(resolveName(jwt, email));
            created.setRole(resolveRole(jwt));
            return userRepository.save(created);
        }

        return u;
    }

    @PutMapping
    public User updateMe(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateRequest req) {
        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("No email claim in token. Add 'email' or custom claim.");
        }

        User u = userRepository.findFirstByEmailIgnoreCase(email.trim());
        if (u == null) {
            u = new User();
            u.setEmail(email.trim());
            u.setRole(resolveRole(jwt));
        }

        if (req != null) {
            if (req.name != null && !req.name.trim().isEmpty()) {
                u.setName(req.name.trim());
            }
            if (req.avatarUrl != null && !req.avatarUrl.trim().isEmpty()) {
                u.setAvatarUrl(req.avatarUrl.trim());
            }
        }

        return userRepository.save(u);
    }

    private String resolveName(Jwt jwt, String email) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String nickname = jwt.getClaimAsString("nickname");
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "User";
    }

    private String resolveRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("https://cooked.api/roles");
        if (roles != null) {
            for (String role : roles) {
                if ("ADMIN".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role)) {
                    return "ADMIN";
                }
            }
        }
        return "USER";
    }

    private String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("https://cooked.api/email");
        }
        return email;
    }

    private static class UpdateRequest {
        public String name;
        public String avatarUrl;
    }
}
