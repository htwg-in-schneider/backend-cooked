package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping({"/api/me", "/api/profile"})
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public User me(@AuthenticationPrincipal Jwt jwt) {
        String oauthId = extractOauthId(jwt);
        if (oauthId != null && !oauthId.isBlank()) {
            User byOauth = userRepository.findFirstByOauthId(oauthId);
            if (byOauth != null) {
                return byOauth;
            }
        }

        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }

        User u = userRepository.findFirstByEmailIgnoreCase(email.trim());
        if (u == null) {
            User created = new User();
            created.setEmail(email.trim());
            created.setName(resolveName(jwt, email));
            created.setRole(resolveRole(jwt));
            created.setOauthId(oauthId);
            return userRepository.save(created);
        }

        if (u.getOauthId() == null && oauthId != null && !oauthId.isBlank()) {
            u.setOauthId(oauthId);
            return userRepository.save(u);
        }
        return u;
    }

    @PutMapping
    public User updateMe(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateRequest req) {
        String oauthId = extractOauthId(jwt);
        if (oauthId != null && !oauthId.isBlank()) {
            User byOauth = userRepository.findFirstByOauthId(oauthId);
            if (byOauth != null) {
                return saveUpdated(byOauth, req);
            }
        }

        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }

        User u = userRepository.findFirstByEmailIgnoreCase(email.trim());
        if (u == null) {
            u = new User();
            u.setEmail(email.trim());
            u.setRole(resolveRole(jwt));
            u.setOauthId(oauthId);
        } else if (u.getOauthId() == null && oauthId != null && !oauthId.isBlank()) {
            u.setOauthId(oauthId);
        }
        return saveUpdated(u, req);
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

    private String extractOauthId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        return jwt.getSubject();
    }

    private User saveUpdated(User u, UpdateRequest req) {
        if (req != null) {
            if (req.name != null && !req.name.trim().isEmpty()) {
                String name = req.name.trim();
                if (name.length() < 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name ist zu kurz (mind. 2 Zeichen)");
                }
                u.setName(name);
            }
            if (req.email != null && !req.email.trim().isEmpty()) {
                String email = req.email.trim();
                if (!email.matches("^\\S+@\\S+\\.\\S+$")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Mail muss gültig sein");
                }
                User other = userRepository.findFirstByEmailIgnoreCase(email);
                if (other != null && !other.getId().equals(u.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diese E-Mail ist bereits vergeben");
                }
                u.setEmail(email);
            }
            if (req.avatarUrl != null && !req.avatarUrl.trim().isEmpty()) {
                u.setAvatarUrl(req.avatarUrl.trim());
            }
        }

        return userRepository.save(u);
    }

    private static class UpdateRequest {
        public String name;
        public String email;
        public String avatarUrl;
    }
}
