package com.jjrpadel.app.model;


import lombok.*;

@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class ParejaConvocada {
    private String jugador1;
    private String jugador2;
    private Integer puntosJ1;
    private Integer puntosJ2;
    private Integer puntosTotal;
}
