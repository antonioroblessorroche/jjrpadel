// src/main/java/com/jjrpadel/app/model/Partido.java
package com.jjrpadel.app.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("partidos")
public class Partido {
    @Id
    private String id;

    private LocalDate fecha;
    private LocalTime hora;

    private String localizacion;
    private Equipo equipo;
    private boolean esSnp;

    @Builder.Default
    private List<Inscripcion> inscripciones = new ArrayList<>();
    private Convocatoria convocatoria;
    private SnpResultado snpResultado;
}

