package com.mfish.erp.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Chaine de securite de l'interface web (rendu serveur) : authentification par formulaire,
 * sessions, CSRF actif, en-tetes de securite stricts et autorisations par role.
 * La chaine API (JWT stateless) reste prioritaire ({@code SecurityConfig}).
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(final HttpSecurity http,
                                                      final UserDetailsService userDetailsService,
                                                      final AuthenticationFailureHandler authenticationFailureHandler)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/connexion", "/app.css", "/app.js", "/vendor/**",
                        "/erreurs/**", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/", "/tournee/**", "/commandes/**", "/clients/**")
                    .hasAnyRole("ADMIN", "LIVREUR")
                .requestMatchers("/produits/**", "/conversations/**", "/connaissance/**",
                        "/statistiques/**", "/parametres/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/connexion")
                .loginProcessingUrl("/connexion")
                .defaultSuccessUrl("/", false)
                .failureHandler(authenticationFailureHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/deconnexion")
                .logoutSuccessUrl("/connexion?deconnexion")
                .permitAll())
            .csrf(Customizer.withDefaults())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; "
                                + "img-src 'self' data:; font-src 'self'; connect-src 'self'; "
                                + "frame-ancestors 'none'; form-action 'self'; base-uri 'self'; "
                                + "object-src 'none'"))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }
}
