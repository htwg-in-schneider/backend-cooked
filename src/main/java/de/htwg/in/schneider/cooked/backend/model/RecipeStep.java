package de.htwg.in.schneider.cooked.backend.model;

import java.util.ArrayList;
import java.util.List;

public class RecipeStep {

    private String text;
    private List<Ingredient> ingredients = new ArrayList<>();

    public RecipeStep() {
    }

    public RecipeStep(String text, List<Ingredient> ingredients) {
        this.text = text;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }
}
