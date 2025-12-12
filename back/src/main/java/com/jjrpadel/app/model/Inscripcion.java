// com/jjrpadel/app/model/Inscripcion.java
package com.jjrpadel.app.model;

import java.time.LocalDateTime;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Inscripcion {
    private String username;
    private LocalDateTime fechaHora;
    private InscripcionEstado estado;
}
