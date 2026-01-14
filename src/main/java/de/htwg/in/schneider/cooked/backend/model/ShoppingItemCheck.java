package de.htwg.in.schneider.cooked.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;

@Entity
@Table(
    name = "shopping_item_checks",
    uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "product_id", "ingredient_key" })
)
public class ShoppingItemCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "ingredient_key", nullable = false, length = 500)
    private String ingredientKey;

    public ShoppingItemCheck() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getIngredientKey() {
        return ingredientKey;
    }

    public void setIngredientKey(String ingredientKey) {
        this.ingredientKey = ingredientKey;
    }
}
