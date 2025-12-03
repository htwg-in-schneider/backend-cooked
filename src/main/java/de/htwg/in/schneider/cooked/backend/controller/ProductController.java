package de.htwg.in.schneider.cooked.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import de.htwg.in.schneider.cooked.backend.model.Category;
import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Optional;

@RestController
// ÄNDERUNG: Wir nennen es "recipes" (Mehrzahl), damit es zum Frontend passt
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public List<Product> getProducts(@RequestParam(required = false) String name,
            @RequestParam(required = false) Category category) {

        if (name != null && category != null) {
            return productRepository.findByTitleContainingIgnoreCaseAndCategory(name, category);
        } else if (name != null) {
            return productRepository.findByTitleContainingIgnoreCase(name);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        } else {
            return productRepository.findAll();
        }
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        if (product.getId() != null) {
            product.setId(null);
        }
        Product newProduct = productRepository.save(product);
        LOG.info("Created new recipe with id " + newProduct.getId());
        return newProduct;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product product = opt.get();

        // Update der Felder
        product.setTitle(productDetails.getTitle());
        product.setDescription(productDetails.getDescription());
        product.setCategory(productDetails.getCategory());
        product.setImageUrl(productDetails.getImageUrl());
        product.setInstructions(productDetails.getInstructions());

        // Zeit statt Preis -> Korrekt!
        product.setPrepTimeMinutes(productDetails.getPrepTimeMinutes());

        Product updatedProduct = productRepository.save(product);
        LOG.info("Updated recipe with id " + updatedProduct.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduct(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        productRepository.delete(opt.get());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}