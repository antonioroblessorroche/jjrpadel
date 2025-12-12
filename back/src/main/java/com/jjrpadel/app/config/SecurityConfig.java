package com.jjrpadel.app.config;

import com.jjrpadel.app.repository.UsuarioRepository;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends VaadinWebSecurity {

    private final UsuarioRepository usuarioRepository;

    public SecurityConfig(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRol().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Desactivar CSRF y HSTS usando la sintaxis moderna (Spring Security 6.1+)
        http.csrf(csrf -> csrf.disable());
        
        http.headers(headers -> headers
            .httpStrictTransportSecurity(hsts -> hsts.disable())
        );

        // Permitir acceso a recursos estáticos y LOGIN explícitamente
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/VAADIN/**", "/favicon.ico", "/robots.txt", "/manifest.webmanifest", "/sw.js", "/offline.html", "/icons/**", "/images/**", "/styles/**", "/h2-console/**").permitAll()
            .requestMatchers(com.vaadin.flow.server.Utils::isFrameworkInternalRequest).permitAll() 
        );

        // Deja que Vaadin configure lo suyo
        super.configure(http);

        // Login view
        setLoginView(http, com.jjrpadel.app.config.LoginView.class);

        // Logout
        http.logout(logout -> logout.logoutSuccessUrl("/login"));
    }
}
