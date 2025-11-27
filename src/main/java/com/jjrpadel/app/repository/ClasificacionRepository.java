// src/main/java/com/jjrpadel/app/repository/ClasificacionRepository.java
package com.jjrpadel.app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jjrpadel.app.model.ClasificacionEntry;

public interface ClasificacionRepository extends MongoRepository<ClasificacionEntry, String> {
    List<ClasificacionEntry> findByGrupoOrderByPuntosDescDsDescDjDesc(String grupo);
    void deleteByGrupo(String grupo);
    List<ClasificacionEntry> findDistinctByOrderByGrupoAsc(); // para listar grupos
}
