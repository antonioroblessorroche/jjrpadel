package com.jjrpadel.app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jjrpadel.app.model.Entrenamiento;
import com.jjrpadel.app.model.Equipo;

public interface EntrenamientoRepository extends MongoRepository<Entrenamiento, String> {
    List<Entrenamiento> findByEquipo(Equipo equipo);
}