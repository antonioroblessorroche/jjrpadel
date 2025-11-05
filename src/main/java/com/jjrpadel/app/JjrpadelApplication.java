package com.jjrpadel.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.UsuarioService;

@SpringBootApplication
public class JjrpadelApplication {

    public static void main(String[] args) {
        SpringApplication.run(JjrpadelApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(UsuarioService usuarioService) {
        return args -> {
            if (usuarioService.count() == 0) {
                // admin por defecto
                usuarioService.saveAdminIfNotExists("admin", "admin", "Admin", "Principal", "A");

            }
        };
    }
}
