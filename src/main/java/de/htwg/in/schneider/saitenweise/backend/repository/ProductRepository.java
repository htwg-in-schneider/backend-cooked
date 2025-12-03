package de.htwg.in.schneider.saitenweise.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.htwg.in.schneider.saitenweise.backend.model.Category;
import de.htwg.in.schneider.saitenweise.backend.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTitleContainingIgnoreCase(String title);
    List<Product> findByCategory(Category category);
    List<Product> findByTitleContainingIgnoreCaseAndCategory(String title, Category category);
}