package de.htwg.in.schneider.cooked.backend.service;

import org.springframework.stereotype.Service;

import de.htwg.in.schneider.cooked.backend.model.Transaction;
import de.htwg.in.schneider.cooked.backend.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void log(
            String action,
            String entityType,
            Long entityId,
            String performedByName,
            String performedByEmail,
            String details
    ) {
        // Fallbacks, damit nix null ist
        if (action == null || action.isBlank()) action = "UNKNOWN";
        if (entityType == null || entityType.isBlank()) entityType = "UNKNOWN";
        if (entityId == null) entityId = -1L;
        if (performedByName == null || performedByName.isBlank()) performedByName = "unknown";
        if (performedByEmail == null || performedByEmail.isBlank()) performedByEmail = "unknown";
        if (details == null) details = "";

        Transaction t = new Transaction();
        t.setAction(action);
        t.setEntityType(entityType);
        t.setEntityId(entityId);
        t.setPerformedByName(performedByName);
        t.setPerformedByEmail(performedByEmail);
        t.setDetails(details);

        transactionRepository.save(t);
    }
}
