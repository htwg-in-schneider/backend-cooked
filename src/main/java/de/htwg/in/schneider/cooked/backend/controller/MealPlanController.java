package de.htwg.in.schneider.cooked.backend.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.in.schneider.cooked.backend.model.MealPlanEntry;
import de.htwg.in.schneider.cooked.backend.model.Ingredient;
import de.htwg.in.schneider.cooked.backend.model.Recipe;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.model.Weekday;
import de.htwg.in.schneider.cooked.backend.repository.MealPlanEntryRepository;
import de.htwg.in.schneider.cooked.backend.repository.RecipeRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/mealplan")
public class MealPlanController {

    private final MealPlanEntryRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final RecipeRepository productRepository;

    public MealPlanController(
            MealPlanEntryRepository mealPlanRepository,
            UserRepository userRepository,
            RecipeRepository productRepository) {
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<MealPlanEntryDto> getPlan(@AuthenticationPrincipal Jwt jwt) {
        User user = requireUser(jwt);
        return mealPlanRepository.findByUser(user).stream()
                .sorted(Comparator.comparingInt(e -> e.getWeekday().getOrder()))
                .map(MealPlanEntryDto::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    public MealPlanEntryDto addEntry(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateRequest req) {
        User user = requireUser(jwt);
        if (req == null || req.productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rezept fehlt");
        }
        Weekday weekday = Weekday.fromString(req.weekday);
        if (weekday == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wochentag fehlt");
        }
        Recipe product = productRepository.findById(req.productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rezept nicht gefunden"));

        MealPlanEntry existing = mealPlanRepository
                .findByUserAndWeekdayAndProduct_Id(user, weekday, product.getId())
                .orElse(null);
        int normalized = normalizeServings(req.servings, product.getServings());
        if (existing != null) {
            int nextServings = (existing.getServings() != null ? existing.getServings() : 0) + normalized;
            if (nextServings <= 0 || nextServings > 1000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portionen sind ungültig");
            }
            existing.setServings(nextServings);
            return MealPlanEntryDto.from(mealPlanRepository.save(existing));
        }

        MealPlanEntry entry = new MealPlanEntry();
        entry.setUser(user);
        entry.setProduct(product);
        entry.setWeekday(weekday);
        entry.setServings(normalized);

        return MealPlanEntryDto.from(mealPlanRepository.save(entry));
    }

    @PutMapping("/{id}")
    public MealPlanEntryDto updateEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody UpdateRequest req) {
        User user = requireUser(jwt);
        MealPlanEntry entry = mealPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Eintrag nicht gefunden"));
        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Berechtigung");
        }

        if (req != null) {
            if (req.weekday != null && !req.weekday.isBlank()) {
                Weekday weekday = Weekday.fromString(req.weekday);
                if (weekday == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wochentag ungültig");
                }
                if (mealPlanRepository.findByUserAndWeekdayAndProduct_Id(user, weekday, entry.getProduct().getId())
                        .filter(existing -> !existing.getId().equals(entry.getId()))
                        .isPresent()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rezept ist an diesem Tag bereits eingeplant");
                }
                entry.setWeekday(weekday);
            }

            if (req.servings != null) {
                entry.setServings(normalizeServings(req.servings, entry.getProduct().getServings()));
            }
        }

        return MealPlanEntryDto.from(mealPlanRepository.save(entry));
    }

    @DeleteMapping("/{id}")
    public void deleteEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        User user = requireUser(jwt);
        MealPlanEntry entry = mealPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Eintrag nicht gefunden"));
        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Berechtigung");
        }
        mealPlanRepository.delete(entry);
    }

    @DeleteMapping
    public void clearPlan(@AuthenticationPrincipal Jwt jwt) {
        User user = requireUser(jwt);
        mealPlanRepository.deleteByUser(user);
    }

    private int normalizeServings(Integer servings, Integer fallback) {
        int base = fallback != null && fallback > 0 ? fallback : 1;
        if (servings == null) {
            return base;
        }
        if (servings <= 0 || servings > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portionen sind ungültig");
        }
        return servings;
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

    private static class CreateRequest {
        public Long productId;
        public String weekday;
        public Integer servings;
    }

    private static class UpdateRequest {
        public String weekday;
        public Integer servings;
    }

    private static class MealPlanEntryDto {
        public Long id;
        public String weekday;
        public Integer servings;
        public ProductDto product;

        public static MealPlanEntryDto from(MealPlanEntry entry) {
            MealPlanEntryDto dto = new MealPlanEntryDto();
            dto.id = entry.getId();
            dto.weekday = entry.getWeekday() != null ? entry.getWeekday().name() : null;
            dto.servings = entry.getServings();
            dto.product = ProductDto.from(entry.getProduct());
            return dto;
        }
    }

    private static class ProductDto {
        public Long id;
        public String title;
        public Integer servings;
        public String imageUrl;
        public List<Ingredient> ingredients;

        public static ProductDto from(Recipe product) {
            ProductDto dto = new ProductDto();
            dto.id = product.getId();
            dto.title = product.getTitle();
            dto.servings = product.getServings();
            dto.imageUrl = product.getImageUrl();
            dto.ingredients = product.getIngredients();
            return dto;
        }
    }
}
