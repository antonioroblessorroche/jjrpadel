package com.jjrpadel.app.repository;

import java.util.List;
import java.util.Optional;

import com.jjrpadel.app.model.Equipo;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.jjrpadel.app.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    List<Usuario> findByEquipo(Equipo equipo);
}
