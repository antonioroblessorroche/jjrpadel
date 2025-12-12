// src/main/java/com/jjrpadel/app/model/ClasificacionEntry.java
package com.jjrpadel.app.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("clasificacion")
public class ClasificacionEntry {
    @Id
    private String id;

    @Indexed
    private String grupo;          // p.ej. "Grupo A", "Grupo B"...

    private String equipo;         // nombre del equipo

    // Totales
    private Integer puntos;
    private Integer j;   // jugados
    private Integer g;   // ganados
    private Integer p;   // perdidos
    private Integer a;   // empatados (si aplica)
    private Integer sg;  // sets ganados
    private Integer sp;  // sets perdidos
    private Integer ds;  // dif. sets
    private Integer jg;  // juegos ganados
    private Integer jp;  // juegos perdidos
    private Integer dj;  // dif. juegos

    // Ida
    private Integer puntosIda;
    private Integer jIda;
    private Integer gIda;
    private Integer pIda;
    private Integer aIda;
    private Integer sgIda;
    private Integer jgIda;

    // Vuelta
    private Integer puntosVuelta;
    private Integer jVuelta;
    private Integer gVuelta;
    private Integer pVuelta;
    private Integer aVuelta;
    private Integer sgVuelta;
    private Integer jgVuelta;
}
