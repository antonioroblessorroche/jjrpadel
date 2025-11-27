// com/jjrpadel/app/model/Entrenamiento.java
package com.jjrpadel.app.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("entrenamientos")
public class Entrenamiento {
    @Id
    private String id;

    private LocalDate fecha;
    private LocalTime hora;

    private String localizacion;
    private Integer pistas;

    private Equipo equipo;
    @Builder.Default
    private List<Inscripcion> inscripciones = new ArrayList<>();
}
