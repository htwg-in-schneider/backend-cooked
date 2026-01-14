package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.in.schneider.cooked.backend.model.Recipe;
import de.htwg.in.schneider.cooked.backend.model.Review;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.RecipeRepository;
import de.htwg.in.schneider.cooked.backend.repository.ReviewRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;
import de.htwg.in.schneider.cooked.backend.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RecipeRepository productRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    private String getActorEmail(HttpServletRequest request, Jwt jwt) {
        String email = extractEmail(jwt);
        if (email != null && !email.isBlank()) {
            return email;
        }
        String headerEmail = request.getHeader("X-User-Email");
        return (headerEmail == null || headerEmail.isBlank()) ? "unknown" : headerEmail;
    }

    private String getActorName(Jwt jwt, String fallback) {
        String name = extractName(jwt);
        if (name != null && !name.isBlank()) {
            return name;
        }
        return (fallback == null || fallback.isBlank()) ? "unknown" : fallback;
    }

    @GetMapping
    public List<Review> getAllReviews() {
        LOG.info("Fetching all reviews");
        List<Review> reviews = reviewRepository.findAll();
        LOG.info("Found {} reviews", reviews != null ? reviews.size() : 0);
        return reviews;
    }

    @GetMapping("/product/{productId}")
    public List<Review> getReviewsByProduct(@PathVariable Long productId) {
        LOG.info("Fetching reviews for product id {}", productId);
        List<Review> reviews = reviewRepository.findByProductId(productId);
        LOG.info("Found {} reviews for product {}", reviews != null ? reviews.size() : 0, productId);
        return reviews;
    }

    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Review review, HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }

        Long productId = null;
        if (review != null && review.getProduct() != null) {
            productId = review.getProduct().getId();
        }
        LOG.info("Attempting to create review for product id {}", productId);

        if (review == null) {
            LOG.warn("Review payload is null");
            return ResponseEntity.badRequest().build();
        }

        User reviewer = resolveUser(jwt);
        if (reviewer != null) {
            review.setUserId(reviewer.getId());
            String name = reviewer.getName();
            if (name != null && !name.isBlank()) {
                review.setUserName(name.trim());
            }
            String email = reviewer.getEmail();
            if (email != null && !email.isBlank()) {
                review.setUserEmail(email.trim());
            }
        } else {
            String jwtEmail = extractEmail(jwt);
            if (jwtEmail != null && !jwtEmail.isBlank()) {
                review.setUserEmail(jwtEmail.trim());
            }
        }

        int stars = review.getStars();
        if (stars < 1 || stars > 5) {
            LOG.warn("Review stars out of bounds: {}", stars);
            return ResponseEntity.badRequest().build();
        }

        if (review.getUserName() == null || review.getUserName().trim().isEmpty()) {
            LOG.warn("Review userName missing");
            return ResponseEntity.badRequest().build();
        }

        if (review.getText() == null || review.getText().trim().isEmpty()) {
            LOG.warn("Review text missing");
            return ResponseEntity.badRequest().build();
        }

        if (review.getProduct() == null || review.getProduct().getId() == null) {
            LOG.warn("Review product is null or has no id");
            return ResponseEntity.badRequest().build();
        }

        Recipe product = productRepository.findById(review.getProduct().getId()).orElse(null);
        if (product == null) {
            LOG.warn("Recipe not found for review: {}", review.getProduct().getId());
            return ResponseEntity.badRequest().build();
        }

        // Review speichern
        review.setProduct(product);
        String avatarUrl = resolveAvatarUrl(jwt, reviewer);
        review.setAvatarUrl(avatarUrl);
        Review saved = reviewRepository.save(review);
        LOG.info("Created review with id {}", saved.getId());

        // TRANSAKTION SPEICHERN (CREATE REVIEW)
        transactionService.log(
                "CREATE",
                "REVIEW",
                saved.getId(),
                getActorName(jwt, saved.getUserName()),
                getActorEmail(request, jwt),
                "Review erstellt: " + stars + " Sterne zu Recipe #" + product.getId()
        );

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteReview(@PathVariable Long id, HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }
        LOG.info("Attempting to delete review with id {}", id);

        User actor = resolveUser(jwt);
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nicht eingeloggt");
        }
        boolean isAdmin = actor.getRole() != null && actor.getRole().equalsIgnoreCase("ADMIN");

        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            LOG.warn("Review not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        }

        if (!isAdmin) {
            boolean isOwner = review.getUserId() != null && review.getUserId().equals(actor.getId());
            if (!isOwner) {
                String actorEmail = actor.getEmail() == null ? "" : actor.getEmail().trim();
                String jwtEmail = extractEmail(jwt);
                if (jwtEmail != null && !jwtEmail.isBlank()) {
                    actorEmail = jwtEmail.trim();
                }
                String reviewEmail = review.getUserEmail() == null ? "" : review.getUserEmail().trim();
                if (!actorEmail.isEmpty() && actorEmail.equalsIgnoreCase(reviewEmail)) {
                    isOwner = true;
                }
            }
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Berechtigung");
            }
        }

        Long productId = (review.getProduct() != null) ? review.getProduct().getId() : null;
        String userName = getActorName(jwt, review.getUserName());

        reviewRepository.delete(review);
        LOG.info("Deleted review with id {}", id);

        // TRANSAKTION SPEICHERN (DELETE REVIEW)
        transactionService.log(
                "DELETE",
                "REVIEW",
                id,
                userName,
                getActorEmail(request, jwt),
                "Review gelöscht (gehörte zu Recipe #" + productId + ")"
        );

        return ResponseEntity.noContent().build();
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

    private String resolveAvatarUrl(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String oauthId = jwt.getSubject();
        if (oauthId != null && !oauthId.isBlank()) {
            User byOauth = userRepository.findFirstByOauthId(oauthId);
            if (byOauth != null && byOauth.getAvatarUrl() != null && !byOauth.getAvatarUrl().isBlank()) {
                return byOauth.getAvatarUrl().trim();
            }
        }
        String email = extractEmail(jwt);
        if (email != null && !email.isBlank()) {
            User byEmail = userRepository.findFirstByEmailIgnoreCase(email.trim());
            if (byEmail != null && byEmail.getAvatarUrl() != null && !byEmail.getAvatarUrl().isBlank()) {
                return byEmail.getAvatarUrl().trim();
            }
        }
        String picture = jwt.getClaimAsString("picture");
        return (picture == null || picture.isBlank()) ? null : picture.trim();
    }

    private User resolveUser(Jwt jwt) {
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

    private String resolveAvatarUrl(Jwt jwt, User reviewer) {
        if (reviewer != null) {
            String url = reviewer.getAvatarUrl();
            return (url == null || url.isBlank()) ? null : url.trim();
        }
        return resolveAvatarUrl(jwt);
    }
}
