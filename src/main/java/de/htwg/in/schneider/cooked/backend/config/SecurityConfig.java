package de.htwg.in.schneider.cooked.backend.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Auth0 Custom Claim (Namespace MUSS URL sein)
    private static final String ROLES_CLAIM = "https://cooked.api/roles";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // Preflight immer erlauben
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/recipes/mine",
                        "/api/recipe/mine",
                        "/api/products/mine",
                        "/api/product/mine").authenticated()
                .requestMatchers("/api/favorites/**").authenticated()

                // Public (dein Frontend lädt Rezepte ohne Login)
                .requestMatchers(HttpMethod.GET, "/api/recipes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/recipe/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/review/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/review/**").authenticated()

                // Profil / Me
                .requestMatchers("/api/me/**").authenticated()
                .requestMatchers("/api/profile/**").authenticated()

                // Admin-only (im Backend prüfen)
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/transactions/**").authenticated()

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> customJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtToAuthorities());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> jwtToAuthorities() {
        return jwt -> {
            // roles claim: ["Admin", "kunde"] oder ["ADMIN"] etc.
            List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
            if (roles == null) roles = List.of();

            return roles.stream()
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .map(r -> r.equalsIgnoreCase("Admin") ? "ADMIN" : r.toUpperCase())
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());
        };
    }
}
