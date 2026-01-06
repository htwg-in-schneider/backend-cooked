package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.cooked.backend.model.Transaction;
import de.htwg.in.schneider.cooked.backend.repository.TransactionRepository;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // GET /api/transactions
    // optional:
    //  - /api/transactions?entityType=PRODUCT
    //  - /api/transactions?performedByEmail=maiermelina04@gmail.com
    @GetMapping
    public List<Transaction> getTransactions(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String performedByEmail
    ) {
        if (performedByEmail != null && !performedByEmail.trim().isEmpty()) {
            return transactionRepository.findByPerformedByEmailOrderByCreatedAtDesc(performedByEmail.trim());
        }
        if (entityType != null && !entityType.trim().isEmpty()) {
            return transactionRepository.findByEntityTypeOrderByCreatedAtDesc(entityType.trim());
        }
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }
}
