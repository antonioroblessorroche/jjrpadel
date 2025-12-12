// src/main/java/com/jjrpadel/app/service/ClasificacionService.java
package com.jjrpadel.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jjrpadel.app.model.ClasificacionEntry;
import com.jjrpadel.app.repository.ClasificacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClasificacionService {
    private final ClasificacionRepository repo;

    public List<ClasificacionEntry> listByGrupo(String grupo) {
        return repo.findByGrupoOrderByPuntosDescDsDescDjDesc(grupo);
    }

    public List<String> listGrupos() {
        return repo.findDistinctByOrderByGrupoAsc()
                   .stream().map(ClasificacionEntry::getGrupo)
                   .distinct().toList();
    }

    /** Reemplaza por completo la clasificación de un grupo */
    public void replaceGrupo(String grupo, List<ClasificacionEntry> rows) {
        repo.deleteByGrupo(grupo);
        // asegúrate de setear el grupo en todas las filas
        var ready = rows.stream().peek(r -> r.setGrupo(grupo)).collect(Collectors.toList());
        repo.saveAll(ready);
    }
}
