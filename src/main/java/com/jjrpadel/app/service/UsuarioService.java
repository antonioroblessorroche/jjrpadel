package com.jjrpadel.app.service;

import java.util.List;
import java.util.Optional;

import com.jjrpadel.app.model.Equipo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public List<Usuario> findAll() { return repo.findAll(); }

    public Usuario save(Usuario u) {
        if (u.getPassword() != null && !u.getPassword().isBlank()) {
            u.setPassword(encoder.encode(u.getPassword()));
        } else if (u.getId() != null) {
            String existing = repo.findById(u.getId()).map(Usuario::getPassword).orElse(null);
            u.setPassword(existing);
        }
        return repo.save(u);
    }

    public void delete(Usuario u) { if (u.getId()!=null) repo.deleteById(u.getId()); }
    public long count() { return repo.count(); }

    public void saveAdminIfNotExists(String username, String rawPassword, String nombre, String apellidos, String equipo) {
        if (!repo.existsByUsername(username)) {
            Usuario admin = Usuario.builder()
                .username(username)
                .password(encoder.encode(rawPassword))
                .nombre(nombre)
                .apellidos(apellidos)
                .rol(Role.ADMIN)
                .puntos(9999)
                .equipo(Equipo.valueOf(equipo))
                .build();
            repo.save(admin);
        }
    }

    public void createIfNotExists(String username, String rawPassword, String nombre, String apellidos, Role role, Integer puntos, String equipo) {
        if (!repo.existsByUsername(username)) {
            Usuario u = Usuario.builder()
                .username(username)
                .password(encoder.encode(rawPassword))
                .nombre(nombre)
                .apellidos(apellidos)
                .rol(role)
                .puntos(puntos)
                .equipo(Equipo.valueOf(equipo))
                .build();
            repo.save(u);
        }
    }

    public Optional<Usuario> findByUsername(String user) {
        return repo.findByUsername(user);
    }

    public Optional<Usuario> findById(String username) {
        return repo.findById(username);
    }
}
