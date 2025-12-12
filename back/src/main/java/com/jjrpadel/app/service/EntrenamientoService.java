// com/jjrpadel/app/service/EntrenamientoService.java
package com.jjrpadel.app.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jjrpadel.app.model.Entrenamiento;
import com.jjrpadel.app.model.Equipo;
import com.jjrpadel.app.model.Inscripcion;
import com.jjrpadel.app.repository.EntrenamientoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntrenamientoService {
    private final EntrenamientoRepository repo;

    public List<Entrenamiento> findAll() { return repo.findAll(); }
    public Optional<Entrenamiento> findById(String id) { return repo.findById(id); }
    public List<Entrenamiento> findByEquipo(Equipo equipo) { return repo.findByEquipo(equipo); }
    public Entrenamiento save(Entrenamiento e) { return repo.save(e); }
    public void deleteById(String id) { repo.deleteById(id); }

    public Entrenamiento apuntar(Entrenamiento e, String username) {
        if (e.getInscripciones() == null) e.setInscripciones(new java.util.ArrayList<>());
        boolean yaApuntado = e.getInscripciones().stream()
                .anyMatch(i -> i.getUsername().equalsIgnoreCase(username));
        if (!yaApuntado) {
            e.getInscripciones().add(Inscripcion.builder()
                    .username(username)
                    .fechaHora(LocalDateTime.now())
                    .build());
            // mantener lista ordenada por fecha/hora (primero el más antiguo)
            e.getInscripciones().sort(Comparator.comparing(Inscripcion::getFechaHora));
            return repo.save(e);
        }
        return e;
    }

    public Entrenamiento apuntarVarios(Entrenamiento e, List<String> usernames) {
        if (e.getInscripciones() == null) {
            e.setInscripciones(new java.util.ArrayList<>());
        }
        for (String username : usernames) {
            boolean ya = e.getInscripciones().stream()
                    .anyMatch(i -> i.getUsername().equalsIgnoreCase(username));
            if (!ya) {
                e.getInscripciones().add(
                        Inscripcion.builder()
                                .username(username)
                                .fechaHora(LocalDateTime.now())
                                .build()
                );
            }
        }
        // Mantener orden por fecha/hora (primero el que se apuntó antes)
        e.getInscripciones().sort(Comparator.comparing(Inscripcion::getFechaHora));
        return repo.save(e);
    }

    public Entrenamiento desapuntar(Entrenamiento e, String username) {
        if (e.getInscripciones() != null) {
            e.getInscripciones().removeIf(i -> i.getUsername().equalsIgnoreCase(username));
            return repo.save(e);
        }
        return e;
    }
}
