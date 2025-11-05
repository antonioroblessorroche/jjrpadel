package com.jjrpadel.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.jjrpadel.app.repository.UsuarioRepository;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends VaadinWebSecurity {
    private final UsuarioRepository usuarioRepository;
    public SecurityConfig(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRol().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);               // Vaadin configura estáticos/endpoints internos

        AuthenticationSuccessHandler successHandler =
            new SimpleUrlAuthenticationSuccessHandler("/usuarios/editar/self");

        http.formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
        );
        setLoginView(http, LoginView.class);
    }
}
