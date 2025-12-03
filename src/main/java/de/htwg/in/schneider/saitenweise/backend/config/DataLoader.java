package de.htwg.in.schneider.saitenweise.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.htwg.in.schneider.saitenweise.backend.model.Category;
import de.htwg.in.schneider.saitenweise.backend.model.Product;
import de.htwg.in.schneider.saitenweise.backend.model.Review;
import de.htwg.in.schneider.saitenweise.backend.repository.ProductRepository;
import de.htwg.in.schneider.saitenweise.backend.repository.ReviewRepository;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Profile("!test")
public class DataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner loadData(ProductRepository repository, ReviewRepository reviewRepository) {
        return args -> {
            if (repository.count() == 0) { // Check if the repository is empty
                LOGGER.info("Database is empty. Loading initial data...");
                loadInitialData(repository, reviewRepository);
            } else {
                LOGGER.info("Database already contains data. Skipping data loading.");
            }
        };
    }

    private void loadInitialData(ProductRepository repository, ReviewRepository reviewRepository) {
        Product violin = new Product();
        violin.setTitle("Geige Modell Paganini");
        violin.setDescription("Eine hochwertige Geige, welche schon alle Konzerthäuser dieser Welt gesehen hat.");
        violin.setCategory(Category.VIOLIN);
        violin.setPrice(1200.00);
        violin.setImageUrl("https://neshanjo.github.io/saitenweise-images/violin_pro.jpg");

        Product doubleBass = new Product();
        doubleBass.setTitle("Kontrabass Modell Maestro");
        doubleBass.setDescription("Ein professioneller Kontrabass, für Klassik- und Jazz geeignet, optimal eingestellt.");
        doubleBass.setCategory(Category.DOUBLE_BASS);
        doubleBass.setPrice(3500.00);
        doubleBass.setImageUrl("https://neshanjo.github.io/saitenweise-images/doublebass_pro.jpg");

        Product strings = new Product();
        strings.setTitle("Geigensaiten Cat Screaming");
        strings.setDescription("Extra dick und robust. Endlich können Sie sich gegen Ihre Katze wehren.");
        strings.setCategory(Category.ACCESSORIES);
        strings.setPrice(30.00);
        strings.setImageUrl("https://neshanjo.github.io/saitenweise-images/accessory_violin_strings.jpg");

        repository.saveAll(Arrays.asList(violin, doubleBass, strings));

        // Add reviews
        Review r1a = new Review();
        r1a.setStars(5);
        r1a.setText("Fantastisches Instrument, klingt wunderbar!");
        r1a.setUserName("Anna");
        r1a.setProduct(violin);
        Review r1b = new Review();
        r1b.setStars(4);
        r1b.setText("Bin ziemlich zufrieden.");
        r1b.setUserName("Oli");
        r1b.setProduct(violin);

        Review r2 = new Review();
        r2.setStars(4);
        r2.setText("Sehr guter Bass, aber schwer zu transportieren.");
        r2.setUserName("Ben");
        r2.setProduct(doubleBass);

        Review r3 = new Review();
        r3.setStars(3);
        r3.setText("Saiten sind ok, aber nicht besonders langlebig.");
        r3.setUserName("Chris");
        r3.setProduct(strings);

        reviewRepository.saveAll(Arrays.asList(r1a, r1b, r2, r3));
        LOGGER.info("Initial data loaded successfully.");
    }
}