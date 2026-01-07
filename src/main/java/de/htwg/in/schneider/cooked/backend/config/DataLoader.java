package de.htwg.in.schneider.cooked.backend.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.htwg.in.schneider.cooked.backend.model.Category;
import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.model.Review;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;
import de.htwg.in.schneider.cooked.backend.repository.ReviewRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@Configuration
@Profile("!test")
public class DataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner loadData(
            ProductRepository repository,
            ReviewRepository reviewRepository,
            UserRepository userRepository) {

        return args -> {
            if (repository.count() == 0) {
                LOGGER.info("Datenbank ist leer. Lade Rezepte...");
                loadInitialData(repository, reviewRepository);

                // Users nur einmal initial laden
                if (userRepository.count() == 0) {
                    LOGGER.info("Lade Test-User...");
                    loadInitialUsers(userRepository);
                }

            } else {
                LOGGER.info("Datenbank enthält bereits Daten.");
            }
        };
    }

    private void loadInitialUsers(UserRepository userRepository) {
        User u1 = new User();
        u1.setName("Melina Maier");
        u1.setEmail("maiermelina04@gmail.com");
        u1.setRole("ADMIN");

        User u2 = new User();
        u2.setName("Anna");
        u2.setEmail("anna@example.com");
        u2.setRole("USER");

        User u3 = new User();
        u3.setName("Ben");
        u3.setEmail("ben@example.com");
        u3.setRole("USER");

        userRepository.saveAll(Arrays.asList(u1, u2, u3));
        LOGGER.info("Test-User erfolgreich geladen.");
    }

    private void loadInitialData(ProductRepository repository, ReviewRepository reviewRepository) {

        Product recipe1 = new Product();
        recipe1.setTitle("Marry Me Chicken Ramen");
        recipe1.setDescription(
                "Hähnchen in Stücke schneiden und anbraten.\n" +
                        "Knoblauch und Chili hinzufügen und kurz mitbraten.\n" +
                        "Brühe und Sahne dazugeben und 5 Minuten köcheln lassen.\n" +
                        "Ramen-Nudeln kochen.\n" +
                        "Nudeln mit der Sauce mischen.\n" +
                        "Mit Frühlingszwiebeln, Ei und Sesam garnieren.");
        recipe1.setCategories(List.of(Category.ASIAN));
        recipe1.setPrepTimeMinutes(40);
        recipe1.setImageUrl("/frontend-cooked/images/essen1.webp");

        // --- REZEPT 2: Spaghetti (Italian) ---
        Product recipe2 = new Product();
        recipe2.setTitle("Spaghetti Bolognese");
        recipe2.setDescription(
                "Zwiebeln und Knoblauch hacken.\n" +
                        "Hackfleisch anbraten.\n" +
                        "Tomatenmark unterrühren.\n" +
                        "Tomaten und Brühe hinzugeben.\n" +
                        "Sauce 20–30 Minuten köcheln lassen.\n" +
                        "Spaghetti al dente kochen.\n" +
                        "Mit Parmesan servieren.");
        recipe2.setCategories(List.of(Category.ITALIAN));
        recipe2.setPrepTimeMinutes(60);
        recipe2.setImageUrl("/frontend-cooked/images/essen3.webp");

        // --- REZEPT 3: Wedges (Vegetarian) ---
        Product recipe3 = new Product();
        recipe3.setTitle("Kartoffelwedges mit Gurkensalat");
        recipe3.setDescription(
                "Kartoffeln in Spalten schneiden.\n" +
                        "Mit Öl und Gewürzen mischen.\n" +
                        "35–40 Minuten im Ofen backen.\n" +
                        "Gurke hobeln und salzen.\n" +
                        "Mit Joghurt, Dill und Zitronensaft vermengen.\n" +
                        "Zusammen servieren.");
        recipe3.setCategories(List.of(Category.VEGETARIAN));
        recipe3.setPrepTimeMinutes(45);
        recipe3.setImageUrl("/frontend-cooked/images/essen2.webp");

        // Rezepte speichern
        repository.saveAll(Arrays.asList(recipe1, recipe2, recipe3));

        // --- REVIEWS (Bewertungen) ---

        // Bewertungen für Marry Me Chicken
        Review r1a = new Review();
        r1a.setStars(5);
        r1a.setText("Unglaublich lecker, mein Freund war begeistert!");
        r1a.setUserName("Anna");
        r1a.setProduct(recipe1);

        Review r1b = new Review();
        r1b.setStars(4);
        r1b.setText("Sehr gut, aber beim nächsten Mal nehme ich weniger Chili.");
        r1b.setUserName("Oli");
        r1b.setProduct(recipe1);

        // Bewertung für Spaghetti
        Review r2 = new Review();
        r2.setStars(5);
        r2.setText("Wie bei Mama in Italien. Perfekt!");
        r2.setUserName("Ben");
        r2.setProduct(recipe2);

        // Bewertung für Wedges
        Review r3 = new Review();
        r3.setStars(3);
        r3.setText("Die Wedges waren okay, aber der Salat brauchte mehr Würze.");
        r3.setUserName("Chris");
        r3.setProduct(recipe3);

        // Reviews speichern
        reviewRepository.saveAll(Arrays.asList(r1a, r1b, r2, r3));

        LOGGER.info("Rezepte und Bewertungen erfolgreich geladen.");
    }
}
