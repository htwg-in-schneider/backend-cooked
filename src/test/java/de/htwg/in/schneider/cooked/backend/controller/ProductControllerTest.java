package de.htwg.in.schneider.cooked.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.htwg.in.schneider.cooked.backend.model.Category;
import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProductController with /api/recipes.
 */
@SpringBootTest
@Profile("test")
public class ProductControllerTest {

        private MockMvc mockMvc;

        @Autowired
        private ProductRepository productRepository;

        @BeforeEach
        public void setUp(WebApplicationContext context) {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                productRepository.deleteAll();
        }

        @Test
        public void testGetProducts() throws Exception {
                Product product = new Product();
                product.setTitle("Spaghetti Carbonara");
                product.setDescription("Ein italienischer Klassiker mit Ei und Speck.");
                product.setCategories(List.of(Category.ITALIAN));
                product.setPrepTimeMinutes(20);
                product.setImageUrl("https://example.com/carbonara.jpg");
                productRepository.save(product);

                mockMvc.perform(get("/api/recipes"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Spaghetti Carbonara"))
                                .andExpect(jsonPath("$[0].description")
                                                .value("Ein italienischer Klassiker mit Ei und Speck."))
                                .andExpect(jsonPath("$[0].categories[0]").value("ITALIAN"))
                                .andExpect(jsonPath("$[0].prepTimeMinutes").value(20))
                                .andExpect(jsonPath("$[0].imageUrl").value("https://example.com/carbonara.jpg"));
        }

        @Test
        public void testGetProductsByName() throws Exception {
                Product p1 = new Product();
                p1.setTitle("Leckere Pizza");
                p1.setDescription("Tomate Mozzarella.");
                p1.setCategories(List.of(Category.ITALIAN));
                p1.setPrepTimeMinutes(30);
                p1.setImageUrl("https://example.com/pizza.jpg");
                productRepository.save(p1);

                Product p2 = new Product();
                p2.setTitle("Leckere Suppe");
                p2.setDescription("Kürbissuppe.");
                p2.setCategories(List.of(Category.VEGETARIAN));
                p2.setPrepTimeMinutes(45);
                p2.setImageUrl("https://example.com/soup.jpg");
                productRepository.save(p2);

                mockMvc.perform(get("/api/recipes").param("name", "Leckere"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Leckere Pizza"))
                                .andExpect(jsonPath("$[1].title").value("Leckere Suppe"))
                                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        public void testGetProductsByCategory() throws Exception {
                Product p1 = new Product();
                p1.setTitle("Pizza Salami");
                p1.setDescription("Mit viel Käse.");
                p1.setCategories(List.of(Category.ITALIAN));
                p1.setPrepTimeMinutes(25);
                p1.setImageUrl("https://example.com/salami.jpg");
                productRepository.save(p1);

                Product p2 = new Product();
                p2.setTitle("Pad Thai");
                p2.setDescription("Nudeln aus Thailand.");
                p2.setCategories(List.of(Category.ASIAN));
                p2.setPrepTimeMinutes(40);
                p2.setImageUrl("https://example.com/padthai.jpg");
                productRepository.save(p2);

                mockMvc.perform(get("/api/recipes").param("category", "ITALIAN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Pizza Salami"))
                                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        public void testGetProductsByNameAndCategory() throws Exception {
                Product p1 = new Product();
                p1.setTitle("Classic Burger");
                p1.setDescription("Rindfleisch Burger.");
                p1.setCategories(List.of(Category.AMERICAN));
                p1.setPrepTimeMinutes(15);
                p1.setImageUrl("https://example.com/burger.jpg");
                productRepository.save(p1);

                Product p2 = new Product();
                p2.setTitle("Modern Burger");
                p2.setDescription("Veganer Burger.");
                p2.setCategories(List.of(Category.AMERICAN));
                p2.setPrepTimeMinutes(20);
                p2.setImageUrl("https://example.com/vegan_burger.jpg");
                productRepository.save(p2);

                mockMvc.perform(get("/api/recipes")
                                .param("name", "Classic")
                                .param("category", "AMERICAN"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Classic Burger"))
                                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        public void testGetProductById() throws Exception {
                Product product = new Product();
                product.setTitle("Lasagne");
                product.setDescription("Schicht für Schicht ein Gedicht.");
                product.setCategories(List.of(Category.ITALIAN));
                product.setPrepTimeMinutes(90);
                product.setImageUrl("https://example.com/lasagne.jpg");
                Long id = productRepository.save(product).getId();

                mockMvc.perform(get("/api/recipes/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Lasagne"))
                                .andExpect(jsonPath("$.description").value("Schicht für Schicht ein Gedicht."))
                                .andExpect(jsonPath("$.categories[0]").value("ITALIAN"))
                                .andExpect(jsonPath("$.prepTimeMinutes").value(90))
                                .andExpect(jsonPath("$.imageUrl").value("https://example.com/lasagne.jpg"));
        }

        @Test
        public void testCreateProduct() throws Exception {
                String payload = """
                                {"title":"Tiramisu","description":"Leckeres Dessert.",
                                 "categories":["DESSERT"],"prepTimeMinutes":30,
                                 "imageUrl":"https://example.com/tiramisu.jpg"}
                                """;

                MvcResult result = mockMvc.perform(post("/api/recipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Tiramisu"))
                                .andExpect(jsonPath("$.categories[0]").value("DESSERT"))
                                .andExpect(jsonPath("$.prepTimeMinutes").value(30))
                                .andReturn();

                JsonNode json = new ObjectMapper().readTree(result.getResponse().getContentAsString());
                Long id = json.get("id").asLong();

                assertNotNull(id);
                assertTrue(productRepository.findById(id).isPresent());
        }

        @Test
        public void testUpdateProduct() throws Exception {
                Product p = new Product();
                p.setTitle("Alt");
                p.setDescription("Alt.");
                p.setCategories(List.of(Category.VEGETARIAN));
                p.setPrepTimeMinutes(1);
                p.setImageUrl("https://example.com/old.jpg");
                Long id = productRepository.save(p).getId();

                String payload = """
                                {"title":"Neu","description":"Besser.",
                                 "categories":["ASIAN"],"prepTimeMinutes":55,
                                 "imageUrl":"https://example.com/new.jpg"}
                                """;

                mockMvc.perform(put("/api/recipes/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Neu"))
                                .andExpect(jsonPath("$.categories[0]").value("ASIAN"));

                Product updated = productRepository.findById(id).orElseThrow();
                assertEquals("Neu", updated.getTitle());
                assertEquals(55, updated.getPrepTimeMinutes());
        }

        @Test
        public void testDeleteProduct() throws Exception {
                Product p = new Product();
                p.setTitle("Delete");
                p.setDescription("To delete.");
                p.setCategories(List.of(Category.ITALIAN));
                p.setPrepTimeMinutes(10);
                p.setImageUrl("https://example.com/d.jpg");
                Long id = productRepository.save(p).getId();

                mockMvc.perform(delete("/api/recipes/" + id))
                                .andExpect(status().isNoContent());

                assertFalse(productRepository.findById(id).isPresent());
        }
}