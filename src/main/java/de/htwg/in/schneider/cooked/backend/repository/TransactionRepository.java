package de.htwg.in.schneider.cooked.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.htwg.in.schneider.cooked.backend.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByOrderByCreatedAtDesc();

    List<Transaction> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    List<Transaction> findByPerformedByEmailOrderByCreatedAtDesc(String performedByEmail);
}
