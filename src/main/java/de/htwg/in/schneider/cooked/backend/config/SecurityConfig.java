package de.htwg.in.schneider.cooked.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@Profile("!test")
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Preflight immer erlauben
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Auth-geschützt
                .requestMatchers("/api/me/**").authenticated()
                .requestMatchers("/api/profile/**").authenticated()
                .requestMatchers("/api/favorites/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/transactions/**").authenticated()
                .requestMatchers("/api/mealplan/**").authenticated()
                .requestMatchers("/api/shopping/**").authenticated()
                .requestMatchers(HttpMethod.GET,
                        "/api/recipes/mine",
                        "/api/recipe/mine",
                        "/api/products/mine",
                        "/api/product/mine").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/review/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/product/**", "/api/products/**", "/api/recipe/**", "/api/recipes/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/product/**", "/api/products/**", "/api/recipe/**", "/api/recipes/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/product/**", "/api/products/**", "/api/recipe/**", "/api/recipes/**").authenticated()

                // Public
                .requestMatchers(HttpMethod.GET, "/api/recipes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/recipe/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/review/**").permitAll()
                .requestMatchers("/api/**").permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
