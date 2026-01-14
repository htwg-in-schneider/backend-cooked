package de.htwg.in.schneider.cooked.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htwg.in.schneider.cooked.backend.model.ShoppingItemCheck;
import de.htwg.in.schneider.cooked.backend.model.User;

public interface ShoppingItemCheckRepository extends JpaRepository<ShoppingItemCheck, Long> {
    List<ShoppingItemCheck> findByUser(User user);
    Optional<ShoppingItemCheck> findByUserAndProductIdAndIngredientKey(User user, Long productId, String ingredientKey);
    void deleteByUser(User user);
    void deleteByProductId(Long productId);
}
