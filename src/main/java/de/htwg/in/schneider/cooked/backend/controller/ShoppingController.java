package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.in.schneider.cooked.backend.model.ShoppingItemCheck;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.ShoppingItemCheckRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/shopping")
public class ShoppingController {

    private final ShoppingItemCheckRepository shoppingRepository;
    private final UserRepository userRepository;

    public ShoppingController(ShoppingItemCheckRepository shoppingRepository, UserRepository userRepository) {
        this.shoppingRepository = shoppingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/checks")
    public List<CheckDto> getChecks(@AuthenticationPrincipal Jwt jwt) {
        User user = requireUser(jwt);
        return shoppingRepository.findByUser(user).stream()
                .map(CheckDto::from)
                .collect(Collectors.toList());
    }

    @PutMapping("/checks")
    public void setCheck(@AuthenticationPrincipal Jwt jwt, @RequestBody CheckRequest req) {
        User user = requireUser(jwt);
        if (req == null || req.productId == null || req.ingredientKey == null || req.ingredientKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Eintrag ungueltig");
        }
        boolean checked = req.checked != null && req.checked;
        shoppingRepository.findByUserAndProductIdAndIngredientKey(user, req.productId, req.ingredientKey)
                .ifPresentOrElse(existing -> {
                    if (!checked) {
                        shoppingRepository.delete(existing);
                    }
                }, () -> {
                    if (checked) {
                        ShoppingItemCheck created = new ShoppingItemCheck();
                        created.setUser(user);
                        created.setProductId(req.productId);
                        created.setIngredientKey(req.ingredientKey.trim());
                        shoppingRepository.save(created);
                    }
                });
    }

    private User requireUser(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }
        User user = loadUser(jwt);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kein Benutzer gefunden");
        }
        return user;
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

    private static class CheckRequest {
        public Long productId;
        public String ingredientKey;
        public Boolean checked;
    }

    private static class CheckDto {
        public Long productId;
        public String ingredientKey;

        public static CheckDto from(ShoppingItemCheck check) {
            CheckDto dto = new CheckDto();
            dto.productId = check.getProductId();
            dto.ingredientKey = check.getIngredientKey();
            return dto;
        }
    }
}
