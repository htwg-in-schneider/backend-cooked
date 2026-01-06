package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
}
