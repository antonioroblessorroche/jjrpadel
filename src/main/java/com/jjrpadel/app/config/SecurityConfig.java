package com.jjrpadel.app.config;

import com.jjrpadel.app.repository.UsuarioRepository;
import com.jjrpadel.app.vaadin.LoginView;
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
        // Deja que Vaadin configure lo suyo (recursos estáticos, endpoints internos…)
        super.configure(http);

        // Esta es LA forma correcta de decirle a Vaadin/Spring qué vista es la de login
        setLoginView(http, LoginView.class);

        // Logout simple: al cerrar sesión vuelve al login
        http.logout(logout -> logout.logoutSuccessUrl("/login"));
    }
}
