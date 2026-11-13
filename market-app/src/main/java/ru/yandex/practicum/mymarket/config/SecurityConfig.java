package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RedirectServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler h = new RedirectServerLogoutSuccessHandler();
        h.setLogoutSuccessUrl(URI.create("/items"));
        return h;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            RedirectServerLogoutSuccessHandler logoutSuccessHandler) {

        DelegatingServerLogoutHandler logoutHandler = new DelegatingServerLogoutHandler(
                new SecurityContextServerLogoutHandler(),
                new WebSessionServerLogoutHandler()
        );

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/login", "/logout", "/register",
                                "/images/**", "/favicon.ico",
                                "/css/**", "/js/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/", "/items", "/items/**")
                        .permitAll()
                        .anyExchange()
                        .authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationSuccessHandler(
                                new RedirectServerAuthenticationSuccessHandler("/items"))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutHandler(logoutHandler)
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .oauth2Client(Customizer.withDefaults());
        return http.build();
    }
}