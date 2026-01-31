package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import de.htwg.in.schneider.cooked.backend.model.Category;
import de.htwg.in.schneider.cooked.backend.model.Ingredient;
import de.htwg.in.schneider.cooked.backend.model.Recipe;
import de.htwg.in.schneider.cooked.backend.model.RecipeStep;
import de.htwg.in.schneider.cooked.backend.repository.MealPlanEntryRepository;
import de.htwg.in.schneider.cooked.backend.repository.RecipeRepository;
import de.htwg.in.schneider.cooked.backend.repository.ReviewRepository;
import de.htwg.in.schneider.cooked.backend.repository.ShoppingItemCheckRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;
import de.htwg.in.schneider.cooked.backend.service.TransactionService;
import de.htwg.in.schneider.cooked.backend.model.User;

@RestController
@RequestMapping({"/api/recipes", "/api/recipe", "/api/products", "/api/product"})
public class RecipeController {

    private static final Logger LOG = LoggerFactory.getLogger(RecipeController.class);

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MealPlanEntryRepository mealPlanEntryRepository;

    @Autowired
    private ShoppingItemCheckRepository shoppingItemCheckRepository;

    @GetMapping
    public List<Recipe> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {

        List<Recipe> base = (name != null && !name.isBlank())
                ? recipeRepository.findByTitleContainingIgnoreCase(name)
                : recipeRepository.findAll();

        Category categoryEnum = parseCategory(category);
        if (categoryEnum == null) {
            return base;
        }

        return base.stream()
                .filter(p -> p.getCategories() != null && p.getCategories().contains(categoryEnum))
                .toList();
    }

    @PostMapping
    public Recipe createProduct(@RequestBody Recipe recipe, @AuthenticationPrincipal Jwt jwt) {
        if (recipe.getId() != null) {
            recipe.setId(null);
        }

        if (recipe.getInstructions() == null && recipe.getDescription() != null) {
            recipe.setInstructions(recipe.getDescription());
        }
        if (recipe.getCreatedByEmail() == null) {
            recipe.setCreatedByEmail(extractEmail(jwt));
        }
        if (recipe.getServings() == null) {
            recipe.setServings(1);
        }

        normalizeProduct(recipe);
        validateProduct(recipe);

        Recipe newRecipe = recipeRepository.save(recipe);

        transactionService.log(
                "CREATE",
                "PRODUCT",
                newRecipe.getId(),
                extractName(jwt),
                extractEmail(jwt),
                "Rezept erstellt: " + newRecipe.getTitle()
        );

        LOG.info("Created new recipe with id {}", newRecipe.getId());
        return newRecipe;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateProduct(
            @PathVariable Long id,
            @RequestBody Recipe recipeDetails,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<Recipe> opt = recipeRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Recipe recipe = opt.get();
        if (!canManage(recipe, jwt)) {
            return ResponseEntity.status(403).build();
        }
        recipe.setTitle(recipeDetails.getTitle());
        recipe.setDescription(recipeDetails.getDescription());
        recipe.setCategories(recipeDetails.getCategories());
        recipe.setImageUrl(recipeDetails.getImageUrl());
        if (recipeDetails.getInstructions() != null) {
            recipe.setInstructions(recipeDetails.getInstructions());
        } else if (recipeDetails.getDescription() != null) {
            recipe.setInstructions(recipeDetails.getDescription());
        }
        recipe.setPrepTimeMinutes(recipeDetails.getPrepTimeMinutes());
        recipe.setServings(recipeDetails.getServings());
        recipe.setIngredients(recipeDetails.getIngredients());
        recipe.setSteps(recipeDetails.getSteps());

        normalizeProduct(recipe);
        validateProduct(recipe);

        Recipe updatedRecipe = recipeRepository.save(recipe);

        transactionService.log(
                "UPDATE",
                "PRODUCT",
                updatedRecipe.getId(),
                extractName(jwt),
                extractEmail(jwt),
                "Rezept bearbeitet: " + updatedRecipe.getTitle()
        );

        LOG.info("Updated recipe with id {}", updatedRecipe.getId());
        return ResponseEntity.ok(updatedRecipe);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Object> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Optional<Recipe> opt = recipeRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Recipe recipe = opt.get();
        if (!canManage(recipe, jwt)) {
            return ResponseEntity.status(403).build();
        }
        removeFromFavorites(recipe);
        reviewRepository.deleteAll(reviewRepository.findByProductId(recipe.getId()));
        mealPlanEntryRepository.deleteByProductId(recipe.getId());
        shoppingItemCheckRepository.deleteByProductId(recipe.getId());
        recipeRepository.delete(recipe);

        transactionService.log(
                "DELETE",
                "PRODUCT",
                recipe.getId(),
                extractName(jwt),
                extractEmail(jwt),
                "Rezept gelöscht: " + recipe.getTitle()
        );

        LOG.info("Deleted recipe with id {}", recipe.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getProductById(@PathVariable Long id) {
        Optional<Recipe> opt = recipeRepository.findById(id);
        return opt.map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/mine")
    public List<Recipe> getMyProducts(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return recipeRepository.findByCreatedByEmailIgnoreCase(email.trim());
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

    private String extractName(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("nickname");
        }
        if ((name == null || name.isBlank()) && extractEmail(jwt) != null) {
            return extractEmail(jwt);
        }
        return name;
    }

    private boolean canManage(Recipe recipe, Jwt jwt) {
        if (jwt == null || recipe == null) {
            return false;
        }
        if (isAdmin(jwt)) {
            return true;
        }
        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            return false;
        }
        String createdBy = recipe.getCreatedByEmail();
        return createdBy != null && createdBy.equalsIgnoreCase(email.trim());
    }

    private boolean isAdmin(Jwt jwt) {
        User u = loadUser(jwt);
        return u != null && u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    private User loadUser(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String oauthId = jwt.getSubject();
        if (oauthId != null && !oauthId.isBlank()) {
            User byOauth = userRepository.findFirstByOauthId(oauthId);
            if (byOauth != null) {
                return byOauth;
            }
        }
        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findFirstByEmailIgnoreCase(email.trim());
    }

    private Category parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Category.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Eingaben trimmen, damit Validierung/DB konsistent bleiben
    private void normalizeProduct(Recipe recipe) {
        if (recipe.getTitle() != null) {
            recipe.setTitle(recipe.getTitle().trim());
        }
        if (recipe.getDescription() != null) {
            recipe.setDescription(recipe.getDescription().trim());
        }
        if (recipe.getInstructions() != null) {
            recipe.setInstructions(recipe.getInstructions().trim());
        }
        if (recipe.getImageUrl() != null) {
            recipe.setImageUrl(recipe.getImageUrl().trim());
        }
        if (recipe.getIngredients() != null) {
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.getName() != null) {
                    ing.setName(ing.getName().trim());
                }
                if (ing.getAmount() != null) {
                    ing.setAmount(ing.getAmount().trim());
                }
            }
        }
        if (recipe.getSteps() != null) {
            for (RecipeStep step : recipe.getSteps()) {
                if (step.getText() != null) {
                    step.setText(step.getText().trim());
                }
                if (step.getTitle() != null) {
                    step.setTitle(step.getTitle().trim());
                }
            }
        }
    }

    // Harte Backend-Validierung (Frontend kann umgangen werden)
    private void validateProduct(Recipe recipe) {
        if (recipe == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produktdaten fehlen");
        }
        String title = recipe.getTitle() != null ? recipe.getTitle().trim() : "";
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titel darf nicht leer sein");
        }
        if (title.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Titel ist zu kurz (mind. 3 Zeichen)");
        }
        if (recipe.getCategories() == null || recipe.getCategories().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindestens eine Kategorie ist erforderlich");
        }
        Integer minutes = recipe.getPrepTimeMinutes();
        if (minutes == null || minutes <= 0 || minutes > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zubereitungszeit ist ungültig");
        }
        Integer servings = recipe.getServings();
        if (servings == null || servings <= 0 || servings > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portionen sind ungültig");
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindestens eine Zutat ist erforderlich");
        }
        boolean hasIngredientName = false;
        for (Ingredient ing : ingredients) {
            String name = ing != null && ing.getName() != null ? ing.getName().trim() : "";
            String amount = ing != null && ing.getAmount() != null ? ing.getAmount().trim() : "";
            if (!name.isEmpty()) {
                hasIngredientName = true;
            }
            if (name.isEmpty() && !amount.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zutat benötigt einen Namen");
            }
        }
        if (!hasIngredientName) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindestens eine Zutat ist erforderlich");
        }

        List<RecipeStep> steps = recipe.getSteps();
        if (steps == null || steps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindestens ein Schritt ist erforderlich");
        }
        boolean hasStepText = false;
        for (RecipeStep step : steps) {
            String text = step != null && step.getText() != null ? step.getText().trim() : "";
            if (!text.isEmpty()) {
                hasStepText = true;
                break;
            }
        }
        if (!hasStepText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mindestens ein Schritt ist erforderlich");
        }
    }

    private void removeFromFavorites(Recipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            return;
        }
        List<User> users = userRepository.findByFavorites_Id(recipe.getId());
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            if (user.getFavorites() != null) {
                user.getFavorites().removeIf(fav -> recipe.getId().equals(fav.getId()));
            }
        }
        userRepository.saveAll(users);
    }
}
