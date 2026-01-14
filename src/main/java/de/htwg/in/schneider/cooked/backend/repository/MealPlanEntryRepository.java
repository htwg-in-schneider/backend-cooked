package de.htwg.in.schneider.cooked.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.htwg.in.schneider.cooked.backend.model.MealPlanEntry;
import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.model.Weekday;

public interface MealPlanEntryRepository extends JpaRepository<MealPlanEntry, Long> {
    List<MealPlanEntry> findByUser(User user);
    Optional<MealPlanEntry> findByUserAndWeekdayAndProduct_Id(User user, Weekday weekday, Long productId);
    void deleteByUser(User user);
    @Modifying
    @Query("delete from MealPlanEntry e where e.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
