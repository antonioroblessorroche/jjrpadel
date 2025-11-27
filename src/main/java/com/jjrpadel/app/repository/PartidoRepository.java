// src/main/java/com/jjrpadel/app/repository/PartidoRepository.java
package com.jjrpadel.app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jjrpadel.app.model.Partido;
import com.jjrpadel.app.model.Equipo;

public interface PartidoRepository extends MongoRepository<Partido, String> {
    List<Partido> findByEquipo(Equipo equipo);
}
