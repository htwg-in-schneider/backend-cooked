package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public FavoritesController(UserRepository userRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Product> getFavorites(@AuthenticationPrincipal Jwt jwt) {
        User user = loadUser(jwt);
        return user.getFavorites();
    }

    @GetMapping("/ids")
    @Transactional(readOnly = true)
    public List<Long> getFavoriteIds(@AuthenticationPrincipal Jwt jwt) {
        User user = loadUser(jwt);
        return user.getFavorites().stream()
                .map(Product::getId)
                .toList();
    }

    @PutMapping("/{productId}")
    @Transactional
    public ResponseEntity<List<Long>> addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId) {
        User user = loadUser(jwt);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = productOpt.get();
        if (user.getFavorites().stream().noneMatch(p -> p.getId().equals(productId))) {
            user.getFavorites().add(product);
        }
        userRepository.save(user);
        return ResponseEntity.ok(user.getFavorites().stream().map(Product::getId).toList());
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public ResponseEntity<List<Long>> removeFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId) {
        User user = loadUser(jwt);
        boolean removed = user.getFavorites()
                .removeIf(p -> p.getId().equals(productId));
        if (!removed) {
            return ResponseEntity.ok(user.getFavorites().stream().map(Product::getId).toList());
        }
        userRepository.save(user);
        return ResponseEntity.ok(user.getFavorites().stream().map(Product::getId).toList());
    }

    private User loadUser(Jwt jwt) {
        String oauthId = extractOauthId(jwt);
        if (oauthId != null && !oauthId.isBlank()) {
            User byOauth = userRepository.findFirstByOauthId(oauthId);
            if (byOauth != null) {
                return byOauth;
            }
        }

        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("No email claim in token. Add 'email' or custom claim.");
        }
        User user = userRepository.findFirstByEmailIgnoreCase(email.trim());
        if (user != null) {
            if (user.getOauthId() == null && oauthId != null && !oauthId.isBlank()) {
                user.setOauthId(oauthId);
                userRepository.save(user);
            }
            return user;
        }

        User created = new User();
        created.setEmail(email.trim());
        created.setName(resolveName(jwt, email));
        created.setRole(resolveRole(jwt));
        created.setOauthId(oauthId);
        return userRepository.save(created);
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
}
