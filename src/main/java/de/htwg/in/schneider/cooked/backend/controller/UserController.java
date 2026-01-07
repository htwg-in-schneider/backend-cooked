package de.htwg.in.schneider.cooked.backend.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
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

    // GET /api/users?search=...
    @GetMapping
    public List<User> getUsers(@RequestParam(required = false) String search) {
        if (!StringUtils.hasText(search)) {
            return userRepository.findAll();
        }

        String q = search.trim();

        List<User> byName = userRepository.findByNameContainingIgnoreCase(q);
        List<User> byEmail = userRepository.findByEmailContainingIgnoreCase(q);

        // Duplikate sauber über ID vermeiden:
        Map<Long, User> merged = new LinkedHashMap<>();
        for (User u : byName) merged.put(u.getId(), u);
        for (User u : byEmail) merged.put(u.getId(), u);

        return new ArrayList<>(merged.values());
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updated) {

        // ---- Backend Validierung ----
        String name = updated.getName() != null ? updated.getName().trim() : "";
        String email = updated.getEmail() != null ? updated.getEmail().trim() : "";
        String role = updated.getRole() != null ? updated.getRole().trim() : "";
        String avatarUrl = updated.getAvatarUrl() != null ? updated.getAvatarUrl().trim() : "";

        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name darf nicht leer sein");
        if (name.length() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name ist zu kurz (mind. 2 Zeichen)");

        if (email.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Mail darf nicht leer sein");
        if (!email.matches("^\\S+@\\S+\\.\\S+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Mail muss gültig sein");

        if (role.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rolle darf nicht leer sein");
        if (!role.equals("ADMIN") && !role.equals("USER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rolle muss ADMIN oder USER sein");
        }

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        // Unique Email check (nur wenn Email geändert wird)
        if (!email.equalsIgnoreCase(existing.getEmail())) {
            User other = userRepository.findFirstByEmailIgnoreCase(email);
            if (other != null && !other.getId().equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diese E-Mail ist bereits vergeben");
            }
        }

        existing.setName(name);
        existing.setEmail(email);
        existing.setRole(role);
        if (!avatarUrl.isEmpty()) {
            existing.setAvatarUrl(avatarUrl);
        }

        return userRepository.save(existing);
    }
}
