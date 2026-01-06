package de.htwg.in.schneider.cooked.backend.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        // Auth0 liefert email nur wenn "email" scope/claim im Token ist
        String email = jwt.getClaimAsString("email");

        // Falls du die Email als Custom Claim setzt:
        if (email == null) {
            email = jwt.getClaimAsString("https://cooked.api/email");
        }

        if (email == null || email.isBlank()) {
            // Dann kann dein Backend User nicht mappen
            // -> in Auth0 Action Email Claim hinzufügen (siehe unten)
            throw new RuntimeException("No email claim in token. Add 'email' or custom claim.");
        }

        User u = userRepository.findFirstByEmailIgnoreCase(email.trim());
        if (u == null) {
            // Optional: wenn User nicht in DB ist -> anlegen oder Fehler
            // Ich würde Fehler werfen, weil ihr laut Aufgabe "pre-created users" habt
            throw new RuntimeException("User not found in DB: " + email);
        }

        return u;
    }
}
