package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import de.htwg.in.schneider.cooked.backend.model.Category;
import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;
import de.htwg.in.schneider.cooked.backend.service.TransactionService;

@RestController
@RequestMapping("/api/recipes")
public class ProductController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {

        List<Product> base = (name != null && !name.isBlank())
                ? productRepository.findByTitleContainingIgnoreCase(name)
                : productRepository.findAll();

        Category categoryEnum = parseCategory(category);
        if (categoryEnum == null) {
            return base;
        }

        return base.stream()
                .filter(p -> p.getCategories() != null && p.getCategories().contains(categoryEnum))
                .toList();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product, @AuthenticationPrincipal Jwt jwt) {
        if (product.getId() != null) {
            product.setId(null);
        }

        if (product.getInstructions() == null && product.getDescription() != null) {
            product.setInstructions(product.getDescription());
        }
        if (product.getCreatedByEmail() == null) {
            product.setCreatedByEmail(extractEmail(jwt));
        }

        Product newProduct = productRepository.save(product);

        transactionService.log(
                "CREATE",
                "PRODUCT",
                newProduct.getId(),
                extractName(jwt),
                extractEmail(jwt),
                "Rezept erstellt: " + newProduct.getTitle()
        );

        LOG.info("Created new recipe with id {}", newProduct.getId());
        return newProduct;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = opt.get();
        if (!canManage(product, jwt)) {
            return ResponseEntity.status(403).build();
        }
        product.setTitle(productDetails.getTitle());
        product.setDescription(productDetails.getDescription());
        product.setCategories(productDetails.getCategories());
        product.setImageUrl(productDetails.getImageUrl());
        if (productDetails.getInstructions() != null) {
            product.setInstructions(productDetails.getInstructions());
        } else if (productDetails.getDescription() != null) {
            product.setInstructions(productDetails.getDescription());
        }
        product.setPrepTimeMinutes(productDetails.getPrepTimeMinutes());
        product.setIngredients(productDetails.getIngredients());
        product.setSteps(productDetails.getSteps());

        Product updatedProduct = productRepository.save(product);

        transactionService.log(
                "UPDATE",
                "PRODUCT",
                updatedProduct.getId(),
                extractName(jwt),
                extractEmail(jwt),
                "Rezept bearbeitet: " + updatedProduct.getTitle()
        );

        LOG.info("Updated recipe with id {}", updatedProduct.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = opt.get();
        if (!canManage(product, jwt)) {
            return ResponseEntity.status(403).build();
        }
        productRepository.delete(product);

        transactionService.log(
                "DELETE",
                "PRODUCT",
                product.getId(),
                extractName(jwt),
                extractEmail(jwt),
                "Rezept gelöscht: " + product.getTitle()
        );

        LOG.info("Deleted recipe with id {}", product.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        return opt.map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/mine")
    public List<Product> getMyProducts(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return productRepository.findByCreatedByEmailIgnoreCase(email.trim());
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

    private boolean canManage(Product product, Jwt jwt) {
        if (jwt == null || product == null) {
            return false;
        }
        if (isAdmin(jwt)) {
            return true;
        }
        String email = extractEmail(jwt);
        if (email == null || email.isBlank()) {
            return false;
        }
        String createdBy = product.getCreatedByEmail();
        return createdBy != null && createdBy.equalsIgnoreCase(email.trim());
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("https://cooked.api/roles");
        if (roles == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "Admin".equalsIgnoreCase(r));
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
}
