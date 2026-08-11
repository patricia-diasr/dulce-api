package com.dulce.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração base de segurança da API.
 *
 * <p>Nesta etapa (setup do projeto) a cadeia de filtros libera todos os endpoints e apenas prepara
 * o terreno (sessão stateless, CSRF desabilitado para API REST). A autenticação via JWT — login sem
 * senha por código enviado por e-mail, com separação de papéis cliente/administrador (RNF04) — será
 * implementada junto com os endpoints de autenticação, em uma etapa posterior.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/docs/**", "/api-docs/**", "/actuator/health")
                    .permitAll()
                    // TODO: restringir por papel (cliente/administrador) quando o
                    // filtro de autenticação JWT for implementado.
                    .anyRequest()
                    .permitAll());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
