package com.jjrpadel.app.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultadoPareja {
    // Nuestros (snapshot desde convocatoria)
    private String  jugador1;
    private String  jugador2;
    private Integer puntosJ1;      // snapshot
    private Integer puntosJ2;      // snapshot
    private Integer totalNuestro;  // snapshot

    private String  nuestros1Nombre;
    private String  nuestros2Nombre;

    // Rivales (introducidos en resultados)
    private String  rival1Nombre;
    private String  rival2Nombre;
    private Integer puntosRival1;
    private Integer puntosRival2;
    private Integer totalRival;

    // 🔢 Resultado del partido (mejor de 3 sets)
    private Integer s1N; // juegos nuestros set 1
    private Integer s1R; // juegos rival set 1
    private Integer s2N;
    private Integer s2R;
    private Integer s3N; // puede ser null si no se jugó
    private Integer s3R;
    private Boolean jugadoTercerSet; // true si se jugó tercer set

    // Totales de sets (derivado pero lo guardamos para consulta rápida)
    private Integer setsGanadosNos;    // 0..3 (normalmente 0..2)
    private Integer setsGanadosRival;  // 0..3
}
