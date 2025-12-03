package de.htwg.in.schneider.cooked.backend.model;

public enum Category {
    // Unsere Rezept-Kategorien
    ITALIAN("Italienisch"),
    ASIAN("Asiatisch"),
    VEGETARIAN("Vegetarisch"),
    AMERICAN("Amerikanisch"),
    DESSERT("Dessert"),
    GERMAN("Deutsch");

    private final String germanName;

    Category(String germanName) {
        this.germanName = germanName;
    }

    public String getGermanName() {
        return germanName;
    }
}