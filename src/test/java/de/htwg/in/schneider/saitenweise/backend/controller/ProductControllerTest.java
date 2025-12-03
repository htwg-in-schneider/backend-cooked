package de.htwg.in.schneider.saitenweise.backend.controller;

import de.htwg.in.schneider.saitenweise.backend.model.Category;
import de.htwg.in.schneider.saitenweise.backend.model.Product;
import de.htwg.in.schneider.saitenweise.backend.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

/**
 * Integration tests for ProductController. Test both the REST endpoints and the database.
 */
@SpringBootTest
@Profile("test")
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
        // Clear the repository before each test
        productRepository.deleteAll();
    }

    @Test
    public void testGetProducts() throws Exception {
        // GIVEN: A product is available in the database
        Product product = new Product();
        product.setTitle("Violin Model Stradivarius");
        product.setDescription("A premium violin for professional musicians.");
        product.setCategory(Category.VIOLIN);
        product.setPrice(1500.0);
        product.setImageUrl("https://example.com/violin.jpg");
        productRepository.save(product);

        // WHEN: All products are requested via REST endpoint
        mockMvc.perform(get("/api/product"))
                // THEN: Status is OK and product details are correct
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Violin Model Stradivarius"))
                .andExpect(jsonPath("$[0].description").value("A premium violin for professional musicians."))
                .andExpect(jsonPath("$[0].category").value("VIOLIN"))
                .andExpect(jsonPath("$[0].price").value(1500.0))
                .andExpect(jsonPath("$[0].imageUrl").value("https://example.com/violin.jpg"));
    }

    @Test
    public void testGetProductsByName() throws Exception {
        // GIVEN: Multiple products in the database
        Product p1 = new Product();
        p1.setTitle("Nice Violin");
        p1.setDescription("A fine acoustic violin.");
        p1.setCategory(Category.VIOLIN);
        p1.setPrice(800.0);
        p1.setImageUrl("https://example.com/violin.jpg");
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setTitle("Nice Cello");
        p2.setDescription("A cool cello.");
        p2.setCategory(Category.CELLO);
        p2.setPrice(1200.0);
        p2.setImageUrl("https://example.com/cello1.jpg");
        productRepository.save(p2);

        Product p3 = new Product();
        p3.setTitle("Ugly Cello");
        p3.setDescription("An ugly cello.");
        p3.setCategory(Category.CELLO);
        p3.setPrice(100000.0);
        p3.setImageUrl("https://example.com/cello2.jpg");
        productRepository.save(p3);

        // WHEN: Products are requested with name filter
        mockMvc.perform(get("/api/product")
                .param("name", "Nice"))
                // THEN: Only the matching product is returned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Nice Violin"))
                .andExpect(jsonPath("$[1].title").value("Nice Cello"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void testGetProductsByCategory() throws Exception {
        // GIVEN: Multiple products in the database
        Product p1 = new Product();
        p1.setTitle("Violin Model A");
        p1.setDescription("A violin.");
        p1.setCategory(Category.VIOLIN);
        p1.setPrice(1000.0);
        p1.setImageUrl("https://example.com/violin_a.jpg");
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setTitle("Cello Model B");
        p2.setDescription("A cello.");
        p2.setCategory(Category.CELLO);
        p2.setPrice(2000.0);
        p2.setImageUrl("https://example.com/cello_b.jpg");
        productRepository.save(p2);

        // WHEN: Products are requested with category filter
        mockMvc.perform(get("/api/product")
                .param("category", "VIOLIN"))
                // THEN: Only the matching product is returned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Violin Model A"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void testGetProductsByNameAndCategory() throws Exception {
        // GIVEN: Multiple products in the database
        Product p1 = new Product();
        p1.setTitle("Classic Violin");
        p1.setDescription("A classic violin.");
        p1.setCategory(Category.VIOLIN);
        p1.setPrice(1100.0);
        p1.setImageUrl("https://example.com/classic_violin.jpg");
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setTitle("Modern Violin");
        p2.setDescription("A modern violin.");
        p2.setCategory(Category.VIOLIN);
        p2.setPrice(1300.0);
        p2.setImageUrl("https://example.com/modern_violin.jpg");
        productRepository.save(p2);

        Product p3 = new Product();
        p3.setTitle("Classic Cello");
        p3.setDescription("A classic cello.");
        p3.setCategory(Category.CELLO);
        p3.setPrice(2100.0);
        p3.setImageUrl("https://example.com/classic_cello.jpg");
        productRepository.save(p3);

        // WHEN: Products are requested with both name and category filter
        mockMvc.perform(get("/api/product")
                .param("name", "Classic")
                .param("category", "VIOLIN"))
                // THEN: Only the matching product is returned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Classic Violin"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void testGetProductById() throws Exception {
        // GIVEN: Product is available in database
        Product product = new Product();
        product.setTitle("Cello Model Virtuoso");
        product.setDescription("A handcrafted cello with a rich, warm tone.");
        product.setCategory(Category.CELLO);
        product.setPrice(2500.0);
        product.setImageUrl("https://example.com/cello.jpg");
        product = productRepository.save(product);

        // WHEN: Product is requested by ID via REST endpoint
        mockMvc.perform(get("/api/product/" + product.getId()))
                .andDo(MockMvcResultHandlers.print())
                // THEN: Status is OK and product details are correct
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cello Model Virtuoso"))
                .andExpect(jsonPath("$.description").value("A handcrafted cello with a rich, warm tone."))
                .andExpect(jsonPath("$.category").value("CELLO"))
                .andExpect(jsonPath("$.price").value(2500.0))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/cello.jpg"));
    }

    @Test
    public void testCreateProduct() throws Exception {
        // GIVEN: No products in the database (ensured by @BeforeEach)

        // WHEN: A new product is created via REST endpoint
        String productPayload = "{\"title\":\"Viola Model Classic\",\"description\":\"A classic viola for beginners.\","
                + "\"category\":\"VIOLA\",\"price\":500.0,\"imageUrl\":\"https://example.com/viola.jpg\"}";

        MvcResult mvcResult = mockMvc.perform(post("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productPayload))
                // THEN (1): Status is OK and product details are correct
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Viola Model Classic"))
                .andExpect(jsonPath("$.description").value("A classic viola for beginners."))
                .andExpect(jsonPath("$.category").value("VIOLA"))
                .andExpect(jsonPath("$.price").value(500.0))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/viola.jpg"))
                .andReturn();

        // THEN (2): The product has an id
        String responseContent = mvcResult.getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(responseContent);
        Long id = json.has("id") && !json.get("id").isNull() ? json.get("id").asLong() : null;
        assertNotNull(id, "created product should have id");

        // THEN (3): And the product has been saved in the database
        Product saved = productRepository.findById(id)
                .orElseThrow(() -> new AssertionError("Saved product not found, id: " + id));
        assertEquals("Viola Model Classic", saved.getTitle());
        assertEquals("A classic viola for beginners.", saved.getDescription());
        assertEquals(Category.VIOLA, saved.getCategory());
        assertEquals(500.0, saved.getPrice());
        assertEquals("https://example.com/viola.jpg", saved.getImageUrl());
    }

    @Test
    public void testUpdateProduct() throws Exception {
        // GIVEN: An existing product in the database
        Product existingProduct = new Product();
        existingProduct.setTitle("Old Cello");
        existingProduct.setDescription("An old cello with a vintage look.");
        existingProduct.setCategory(Category.CELLO);
        existingProduct.setPrice(300.0);
        existingProduct.setImageUrl("https://example.com/old_cello.jpg");
        Long id = productRepository.save(existingProduct).getId();

        // WHEN: The product is updated via REST endpoint
        String updatePayload = "{\"title\":\"Updated Cello\",\"description\":\"A refurbished cello with modern features.\","
                + "\"category\":\"CELLO\",\"price\":700.0,\"imageUrl\":\"https://example.com/updated_cello.jpg\"}";
        mockMvc.perform(put("/api/product/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                // THEN (1): Status is OK and updated product details are correct
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Cello"))
                .andExpect(jsonPath("$.description").value("A refurbished cello with modern features."))
                .andExpect(jsonPath("$.category").value("CELLO"))
                .andExpect(jsonPath("$.price").value(700.0))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/updated_cello.jpg"));

        // THEN (2): The product is updated in the database
        Product updated = productRepository.findById(id)
                .orElseThrow(() -> new AssertionError("Updated product not found, id: " + id));
        assertEquals("Updated Cello", updated.getTitle());
        assertEquals("A refurbished cello with modern features.", updated.getDescription());
        assertEquals(Category.CELLO, updated.getCategory());
        assertEquals(700.0, updated.getPrice());
        assertEquals("https://example.com/updated_cello.jpg", updated.getImageUrl());
    }

    @Test
    public void testDeleteProduct() throws Exception {
        // GIVEN: A product exists in the database
        Product product = new Product();
        product.setTitle("To Be Deleted");
        product.setDescription("This product will be deleted.");
        product.setCategory(Category.ACCESSORIES);
        product.setPrice(50.0);
        product.setImageUrl("https://example.com/to_be_deleted.jpg");
        product = productRepository.save(product);

        // WHEN: The product is deleted via REST endpoint
        mockMvc.perform(delete("/api/product/" + product.getId()))
                // THEN (1): Status is No Content
                .andExpect(status().isNoContent());

        // THEN (2): The product is no longer in the database
        Optional<Product> deleted = productRepository.findById(product.getId());
        assertFalse(deleted.isPresent(), "Product should have been deleted");
    }
}