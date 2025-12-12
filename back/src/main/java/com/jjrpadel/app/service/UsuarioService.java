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
        // Si es actualización, recuperar el existente para preservar password si no cambia
        if (u.getId() != null) {
            return repo.findById(u.getId())
                    .map(existing -> {
                        String incomingPw = u.getPassword();

                        if (incomingPw == null || incomingPw.isBlank()) {
                            u.setPassword(existing.getPassword());
                        } else if (isBCrypt(incomingPw)) {
                            u.setPassword(incomingPw);
                        } else {
                            u.setPassword(encoder.encode(incomingPw));
                        }

                        return repo.save(u);
                    })
                    .orElseGet(() -> {
                        u.setPassword(encodeIfNeeded(u.getPassword()));
                        return repo.save(u);
                    });
        } else {
            u.setPassword(encodeIfNeeded(u.getPassword()));
            return repo.save(u);
        }
    }

    private String encodeIfNeeded(String raw) {
        if (raw == null || raw.isBlank()) return raw;      // en altas, mejor validar antes que no sea null
        return isBCrypt(raw) ? raw : encoder.encode(raw);
    }

    private boolean isBCrypt(String pw) {
        // Hashes de BCrypt típicos
        return pw != null && (pw.startsWith("$2a$") || pw.startsWith("$2b$") || pw.startsWith("$2y$"));
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

    public List<Usuario> findByEquipo(Equipo equipo) {
        return repo.findByEquipo(equipo);
    }
}
