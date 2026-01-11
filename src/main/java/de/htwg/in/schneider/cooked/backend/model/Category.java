package de.htwg.in.schneider.cooked.backend.model;

public enum Category {
    // Unsere Rezept-Kategorien
    ITALIAN("Italienisch"),
    ASIAN("Asiatisch"),
    VEGETARIAN("Vegetarisch"),
    VEGAN("Vegan"),
    AMERICAN("Amerikanisch"),
    DESSERT("Dessert"),
    GERMAN("Deutsch"),
    MEDITERRANEAN("Mediterran"),
    MEXICAN("Mexikanisch"),
    INDIAN("Indisch"),
    FRENCH("Französisch"),
    SPANISH("Spanisch"),
    MIDDLE_EASTERN("Orientalisch"),
    THAI("Thailändisch"),
    CHINESE("Chinesisch"),
    JAPANESE("Japanisch"),
    BREAKFAST("Frühstück"),
    SOUP("Suppe"),
    SALAD("Salat"),
    PASTA("Pasta"),
    BAKING("Backen"),
    GRILL("Grillen"),
    SEAFOOD("Fisch/Meeresfrüchte"),
    MEAT("Fleisch"),
    SIDE("Beilage"),
    MAIN("Hauptgericht"),
    APPETIZER("Vorspeise"),
    SNACK("Snack"),
    DRINKS("Getränke");

    private final String germanName;

    Category(String germanName) {
        this.germanName = germanName;
    }

    public String getGermanName() {
        return germanName;
    }
}
