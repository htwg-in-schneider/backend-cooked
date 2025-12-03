package de.htwg.in.schneider.saitenweise.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import de.htwg.in.schneider.saitenweise.backend.model.Review;
import de.htwg.in.schneider.saitenweise.backend.model.Product;
import de.htwg.in.schneider.saitenweise.backend.repository.ReviewRepository;
import de.htwg.in.schneider.saitenweise.backend.repository.ProductRepository;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

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
    public ResponseEntity<Review> createReview(@RequestBody Review review) {
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

        if (review.getProduct() == null || review.getProduct().getId() == null) {
            LOG.warn("Review product is null or has no id");
            return ResponseEntity.badRequest().build();
        }

        Product product = productRepository.findById(review.getProduct().getId()).orElse(null);
        if (product == null) {
            LOG.warn("Product not found for review: {}", review.getProduct().getId());
            return ResponseEntity.badRequest().build();
        }

        review.setProduct(product);
        Review saved = reviewRepository.save(review);
        LOG.info("Created review with id {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteReview(@PathVariable Long id) {
        LOG.info("Attempting to delete review with id {}", id);
        Review review = reviewRepository.findById(id).orElse(null);
        if (review != null) {
            reviewRepository.delete(review);
            LOG.info("Deleted review with id {}", id);
            return ResponseEntity.noContent().build();
        } else {
            LOG.warn("Review not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
