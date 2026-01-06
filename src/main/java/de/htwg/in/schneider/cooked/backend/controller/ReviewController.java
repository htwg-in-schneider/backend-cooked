package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.model.Review;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;
import de.htwg.in.schneider.cooked.backend.repository.ReviewRepository;
import de.htwg.in.schneider.cooked.backend.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/review")
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionService transactionService;

    private String getActorEmail(HttpServletRequest request) {
        String email = request.getHeader("X-User-Email");
        return (email == null || email.isBlank()) ? "unknown" : email;
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
    public ResponseEntity<Review> createReview(@RequestBody Review review, HttpServletRequest request) {

        Long productId = null;
        if (review != null && review.getProduct() != null) {
            productId = review.getProduct().getId();
        }
        LOG.info("Attempting to create review for product id {}", productId);

        if (review == null) {
            LOG.warn("Review payload is null");
            return ResponseEntity.badRequest().build();
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

        Product product = productRepository.findById(review.getProduct().getId()).orElse(null);
        if (product == null) {
            LOG.warn("Product not found for review: {}", review.getProduct().getId());
            return ResponseEntity.badRequest().build();
        }

        // Review speichern
        review.setProduct(product);
        Review saved = reviewRepository.save(review);
        LOG.info("Created review with id {}", saved.getId());

        // ✅ TRANSAKTION SPEICHERN (CREATE REVIEW)
        transactionService.log(
                "CREATE",
                "REVIEW",
                saved.getId(),
                saved.getUserName() != null ? saved.getUserName().trim() : "unknown",
                getActorEmail(request),
                "Review erstellt: " + stars + " Sterne zu Product #" + product.getId()
        );

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteReview(@PathVariable Long id, HttpServletRequest request) {
        LOG.info("Attempting to delete review with id {}", id);

        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            LOG.warn("Review not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        }

        Long productId = (review.getProduct() != null) ? review.getProduct().getId() : null;
        String userName = (review.getUserName() != null && !review.getUserName().isBlank())
                ? review.getUserName().trim()
                : "unknown";

        reviewRepository.delete(review);
        LOG.info("Deleted review with id {}", id);

        // ✅ TRANSAKTION SPEICHERN (DELETE REVIEW)
        transactionService.log(
                "DELETE",
                "REVIEW",
                id,
                userName,
                getActorEmail(request),
                "Review gelöscht (gehörte zu Product #" + productId + ")"
        );

        return ResponseEntity.noContent().build();
    }
}
