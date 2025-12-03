package de.htwg.in.schneider.saitenweise.backend.model;

public enum Category {
    VIOLIN("Violine"),
    VIOLA("Bratsche"),
    CELLO("Cello"),
    DOUBLE_BASS("Kontrabass"),
    ACCESSORIES("Zubehör");

    private final String germanName;

    Category(String germanName) {
        this.germanName = germanName;
    }

    public String getGermanName() {
        return germanName;
    }
}