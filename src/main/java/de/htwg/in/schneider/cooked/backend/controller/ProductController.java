package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

        if (product.getInstructions() == null && product.getDescription() != null) {
            product.setInstructions(product.getDescription());
        }

        Product newProduct = productRepository.save(product);

        transactionService.log(
                "CREATE",
                "PRODUCT",
                newProduct.getId(),
                "unknown",
                "unknown",
                "Rezept erstellt: " + newProduct.getTitle()
        );

        LOG.info("Created new recipe with id {}", newProduct.getId());
        return newProduct;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {

        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = opt.get();
        product.setTitle(productDetails.getTitle());
        product.setDescription(productDetails.getDescription());
        product.setCategory(productDetails.getCategory());
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
                "unknown",
                "unknown",
                "Rezept bearbeitet: " + updatedProduct.getTitle()
        );

        LOG.info("Updated recipe with id {}", updatedProduct.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduct(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = opt.get();
        productRepository.delete(product);

        transactionService.log(
                "DELETE",
                "PRODUCT",
                product.getId(),
                "unknown",
                "unknown",
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
}
