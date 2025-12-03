package de.htwg.in.schneider.saitenweise.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import de.htwg.in.schneider.saitenweise.backend.model.Category;
import de.htwg.in.schneider.saitenweise.backend.model.Product;
import de.htwg.in.schneider.saitenweise.backend.repository.ProductRepository;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/product")
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
            LOG.warn("Attempted to create a product with an existing ID. ID has been set to null to create a new product.");
        }
        Product newProduct = productRepository.save(product);
        LOG.info("Created new product with id " + newProduct.getId());
        return newProduct;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        Optional<Product> opt = productRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Product product = opt.get();
        product.setCategory(productDetails.getCategory());
        product.setDescription(productDetails.getDescription());
        product.setImageUrl(productDetails.getImageUrl());
        product.setPrice(productDetails.getPrice());
        product.setTitle(productDetails.getTitle());
        Product updatedProduct = productRepository.save(product);
        LOG.info("Updated product with id " + updatedProduct.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduct(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        productRepository.delete(opt.get());
        LOG.info("Deleted product with id " + id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}