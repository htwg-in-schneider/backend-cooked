package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.in.schneider.cooked.backend.model.Transaction;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.TransactionRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionController(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // GET /api/transactions
    // optional:
    //  - /api/transactions?entityType=PRODUCT
    //  - /api/transactions?performedByEmail=maiermelina04@gmail.com
    @GetMapping
    public List<Transaction> getTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String performedByEmail
    ) {
        requireAdmin(jwt);
        if (performedByEmail != null && !performedByEmail.trim().isEmpty()) {
            return transactionRepository.findByPerformedByEmailOrderByCreatedAtDesc(performedByEmail.trim());
        }
        if (entityType != null && !entityType.trim().isEmpty()) {
            return transactionRepository.findByEntityTypeOrderByCreatedAtDesc(entityType.trim());
        }
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }
        if (hasAdminRole(jwt)) {
            return;
        }
        User u = loadUser(jwt);
        if (u == null || u.getRole() == null || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Berechtigung");
        }
    }

    private boolean hasAdminRole(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("https://cooked.api/roles");
        if (roles == null) {
            roles = jwt.getClaimAsStringList("roles");
        }
        if (roles == null) {
            return false;
        }
        for (String role : roles) {
            if ("ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    private User loadUser(Jwt jwt) {
        String oauthId = jwt.getSubject();
        if (oauthId != null && !oauthId.isBlank()) {
            User byOauth = userRepository.findFirstByOauthId(oauthId);
            if (byOauth != null) {
                return byOauth;
            }
        }
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("https://cooked.api/email");
        }
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findFirstByEmailIgnoreCase(email.trim());
    }
}
