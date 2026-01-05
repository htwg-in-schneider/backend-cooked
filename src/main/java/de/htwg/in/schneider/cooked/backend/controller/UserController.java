package de.htwg.in.schneider.cooked.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.htwg.in.schneider.cooked.backend.model.User;
import de.htwg.in.schneider.cooked.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1) Anzeigen + (optional) suchen
    // Beispiele:
    //  - GET /api/users
    //  - GET /api/users?search=melina
    @GetMapping
    public List<User> getUsers(@RequestParam(required = false) String search) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findAll();
        }

        String q = search.trim();

        // einfache Suche über Name UND Email (zusammengeführt)
        List<User> byName = userRepository.findByNameContainingIgnoreCase(q);
        List<User> byEmail = userRepository.findByEmailContainingIgnoreCase(q);

        // Duplikate vermeiden (falls ein User in beiden Listen wäre)
        byEmail.stream().filter(u -> !byName.contains(u)).forEach(byName::add);

        return byName;
    }

    // 2) Bearbeiten (Admin kann User ändern)
    // PUT /api/users/5
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updated) {
        // Backend-Validierung (manuell, weil wir ohne validation-dependency arbeiten)
        if (updated.getName() == null || updated.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name darf nicht leer sein");
        }
        if (updated.getEmail() == null || updated.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Mail darf nicht leer sein");
        }
        if (!updated.getEmail().contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Mail muss gültig sein");
        }
        if (updated.getRole() == null || updated.getRole().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rolle darf nicht leer sein");
        }

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        existing.setName(updated.getName().trim());
        existing.setEmail(updated.getEmail().trim());
        existing.setRole(updated.getRole().trim());

        return userRepository.save(existing);
    }
}
