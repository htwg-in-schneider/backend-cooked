package de.htwg.in.schneider.cooked.backend.model;

public enum Weekday {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    private final int order;

    Weekday(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public static Weekday fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Weekday.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
