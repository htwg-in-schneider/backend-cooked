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
import de.htwg.in.schneider.cooked.backend.model.Ingredient;
import de.htwg.in.schneider.cooked.backend.model.Product;
import de.htwg.in.schneider.cooked.backend.model.RecipeStep;
import de.htwg.in.schneider.cooked.backend.model.Review;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.ProductRepository;
import de.htwg.in.schneider.cooked.backend.repository.ReviewRepository;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@Configuration
@Profile({ "local", "dev", "prod" })
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
        recipe1.setDescription("Cremige Ramen mit gebratenem Hähnchen und leichter Chili-Schärfe.");
        recipe1.setInstructions(
                "Hähnchen anbraten, Sauce kochen, Nudeln garen und alles zusammenführen.");
        recipe1.setCategories(List.of(Category.ASIAN));
        recipe1.setPrepTimeMinutes(40);
        recipe1.setImageUrl("/images/essen1.webp");

        Ingredient ramenChicken = new Ingredient("Hähnchenbrust", "300 g");
        Ingredient ramenNoodles = new Ingredient("Ramen-Nudeln", "2 Portionen");
        Ingredient ramenGarlic = new Ingredient("Knoblauch", "2 Zehen");
        Ingredient ramenChili = new Ingredient("Chiliflocken", "1/2 TL");
        Ingredient ramenBroth = new Ingredient("Hähnchenbrühe", "500 ml");
        Ingredient ramenCream = new Ingredient("Sahne", "150 ml");
        Ingredient ramenSoy = new Ingredient("Sojasauce", "1 EL");
        Ingredient ramenParmesan = new Ingredient("Parmesan", "30 g");
        Ingredient ramenOil = new Ingredient("Öl", "1 EL");
        Ingredient ramenSalt = new Ingredient("Salz", "nach Geschmack");
        Ingredient ramenOnion = new Ingredient("Frühlingszwiebeln", "2 Stangen");
        Ingredient ramenSesame = new Ingredient("Sesam", "1 TL");
        Ingredient ramenEggs = new Ingredient("Eier", "2");

        recipe1.setIngredients(List.of(
                ramenChicken,
                ramenNoodles,
                ramenGarlic,
                ramenChili,
                ramenBroth,
                ramenCream,
                ramenSoy,
                ramenParmesan,
                ramenOil,
                ramenSalt,
                ramenOnion,
                ramenSesame,
                ramenEggs));

        recipe1.setSteps(List.of(
                new RecipeStep("Hähnchen in Streifen schneiden, salzen und in Öl goldbraun anbraten.",
                        List.of(ramenChicken, ramenOil, ramenSalt)),
                new RecipeStep("Knoblauch und Chiliflocken kurz mitrösten.",
                        List.of(ramenGarlic, ramenChili)),
                new RecipeStep("Brühe, Sahne, Sojasauce und Parmesan einrühren und 5-7 Minuten köcheln.",
                        List.of(ramenBroth, ramenCream, ramenSoy, ramenParmesan)),
                new RecipeStep("Ramen-Nudeln nach Packung garen und abtropfen lassen.",
                        List.of(ramenNoodles)),
                new RecipeStep("Nudeln und Hähnchen in die Sauce geben und kurz ziehen lassen.",
                        List.of(ramenNoodles, ramenChicken)),
                new RecipeStep("Mit Frühlingszwiebeln, Ei und Sesam garnieren.",
                        List.of(ramenOnion, ramenEggs, ramenSesame))));

        // --- REZEPT 2: Spaghetti (Italian) ---
        Product recipe2 = new Product();
        recipe2.setTitle("Spaghetti Bolognese");
        recipe2.setDescription("Klassische Bolognese mit Gemüse, Rinderhack und Parmesan.");
        recipe2.setInstructions("Sauce langsam köcheln lassen und mit Spaghetti servieren.");
        recipe2.setCategories(List.of(Category.ITALIAN));
        recipe2.setPrepTimeMinutes(60);
        recipe2.setImageUrl("/images/essen3.webp");

        Ingredient boloSpaghetti = new Ingredient("Spaghetti", "250 g");
        Ingredient boloMeat = new Ingredient("Rinderhackfleisch", "300 g");
        Ingredient boloOnion = new Ingredient("Zwiebel", "1");
        Ingredient boloGarlic = new Ingredient("Knoblauch", "2 Zehen");
        Ingredient boloCarrot = new Ingredient("Karotte", "1");
        Ingredient boloCelery = new Ingredient("Selleriestange", "1");
        Ingredient boloTomatoPaste = new Ingredient("Tomatenmark", "2 EL");
        Ingredient boloTomatoes = new Ingredient("Passierte Tomaten", "400 g");
        Ingredient boloBroth = new Ingredient("Rinderbrühe", "150 ml");
        Ingredient boloOil = new Ingredient("Olivenöl", "1 EL");
        Ingredient boloOregano = new Ingredient("Oregano", "1 TL");
        Ingredient boloSalt = new Ingredient("Salz", "nach Geschmack");
        Ingredient boloPepper = new Ingredient("Pfeffer", "nach Geschmack");
        Ingredient boloParmesan = new Ingredient("Parmesan", "30 g");

        recipe2.setIngredients(List.of(
                boloSpaghetti,
                boloMeat,
                boloOnion,
                boloGarlic,
                boloCarrot,
                boloCelery,
                boloTomatoPaste,
                boloTomatoes,
                boloBroth,
                boloOil,
                boloOregano,
                boloSalt,
                boloPepper,
                boloParmesan));

        recipe2.setSteps(List.of(
                new RecipeStep("Zwiebel, Knoblauch, Karotte und Sellerie fein würfeln.",
                        List.of(boloOnion, boloGarlic, boloCarrot, boloCelery)),
                new RecipeStep("Öl erhitzen, Gemüse anschwitzen und Hackfleisch bräunen.",
                        List.of(boloOil, boloMeat, boloOnion, boloGarlic, boloCarrot, boloCelery)),
                new RecipeStep("Tomatenmark kurz anrösten, Tomaten und Brühe zugeben, würzen.",
                        List.of(boloTomatoPaste, boloTomatoes, boloBroth, boloOregano, boloSalt, boloPepper)),
                new RecipeStep("Sauce 20-30 Minuten bei kleiner Hitze köcheln lassen.",
                        List.of(boloTomatoes, boloBroth)),
                new RecipeStep("Spaghetti in Salzwasser al dente kochen.",
                        List.of(boloSpaghetti, boloSalt)),
                new RecipeStep("Mit Parmesan servieren.",
                        List.of(boloParmesan))));

        // --- REZEPT 3: Wedges (Vegetarian) ---
        Product recipe3 = new Product();
        recipe3.setTitle("Kartoffelwedges mit Gurkensalat");
        recipe3.setDescription("Knusprige Ofenwedges mit frischem Gurkensalat.");
        recipe3.setInstructions("Wedges backen und den Salat währenddessen anrühren.");
        recipe3.setCategories(List.of(Category.VEGETARIAN));
        recipe3.setPrepTimeMinutes(45);
        recipe3.setImageUrl("/images/essen2.webp");

        Ingredient wedgePotatoes = new Ingredient("Kartoffeln", "600 g");
        Ingredient wedgeOil = new Ingredient("Öl", "2 EL");
        Ingredient wedgePaprika = new Ingredient("Paprikapulver", "1 TL");
        Ingredient wedgeGarlic = new Ingredient("Knoblauchpulver", "1/2 TL");
        Ingredient wedgeSalt = new Ingredient("Salz", "nach Geschmack");
        Ingredient wedgePepper = new Ingredient("Pfeffer", "nach Geschmack");
        Ingredient saladCucumber = new Ingredient("Gurke", "1");
        Ingredient saladYogurt = new Ingredient("Joghurt", "150 g");
        Ingredient saladDill = new Ingredient("Dill", "1 EL");
        Ingredient saladLemon = new Ingredient("Zitronensaft", "1 EL");
        Ingredient saladSugar = new Ingredient("Zucker", "1/2 TL");

        recipe3.setIngredients(List.of(
                wedgePotatoes,
                wedgeOil,
                wedgePaprika,
                wedgeGarlic,
                wedgeSalt,
                wedgePepper,
                saladCucumber,
                saladYogurt,
                saladDill,
                saladLemon,
                saladSugar));

        recipe3.setSteps(List.of(
                new RecipeStep("Backofen auf 200 C vorheizen.", List.of()),
                new RecipeStep("Kartoffeln in Spalten schneiden und mit Öl und Gewürzen mischen.",
                        List.of(wedgePotatoes, wedgeOil, wedgePaprika, wedgeGarlic, wedgeSalt, wedgePepper)),
                new RecipeStep("Wedges 35-40 Minuten backen, einmal wenden.",
                        List.of(wedgePotatoes)),
                new RecipeStep("Gurke dünn hobeln, salzen und 10 Minuten ziehen lassen.",
                        List.of(saladCucumber, wedgeSalt)),
                new RecipeStep("Joghurt mit Dill, Zitronensaft und Zucker verrühren.",
                        List.of(saladYogurt, saladDill, saladLemon, saladSugar)),
                new RecipeStep("Gurke ausdrücken, mit dem Dressing mischen und mit den Wedges servieren.",
                        List.of(saladCucumber, saladYogurt))));

        // Rezepte speichern
        repository.saveAll(Arrays.asList(recipe1, recipe2, recipe3));

        // --- REVIEWS (Bewertungen) ---

        // Bewertungen für Marry Me Chicken
        Review r1a = new Review();
        r1a.setStars(5);
        r1a.setText("Cremig und perfekt würzig, die Sauce passt super zu Ramen.");
        r1a.setUserName("Anna");
        r1a.setProduct(recipe1);

        Review r1b = new Review();
        r1b.setStars(4);
        r1b.setText("Sehr lecker, nächstes Mal nehme ich weniger Chili.");
        r1b.setUserName("Oli");
        r1b.setProduct(recipe1);

        // Bewertungen für Spaghetti
        Review r2a = new Review();
        r2a.setStars(5);
        r2a.setText("Schmeckt wie beim Italiener, schön sossig.");
        r2a.setUserName("Ben");
        r2a.setProduct(recipe2);

        Review r2b = new Review();
        r2b.setStars(4);
        r2b.setText("Gute Alltags-Bolognese mit frischem Gemüse.");
        r2b.setUserName("Lena");
        r2b.setProduct(recipe2);

        // Bewertungen für Wedges
        Review r3a = new Review();
        r3a.setStars(5);
        r3a.setText("Knusprige Wedges und frischer Salat, top Kombination.");
        r3a.setUserName("Chris");
        r3a.setProduct(recipe3);

        Review r3b = new Review();
        r3b.setStars(4);
        r3b.setText("Gurkensalat war erfrischend, ich würde mehr Dill nehmen.");
        r3b.setUserName("Mila");
        r3b.setProduct(recipe3);

        // Reviews speichern
        reviewRepository.saveAll(Arrays.asList(r1a, r1b, r2a, r2b, r3a, r3b));

        LOGGER.info("Rezepte und Bewertungen erfolgreich geladen.");
    }
}
