package com.jjrpadel.app.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Convocatoria {
    private LocalDateTime creadaEn;
    @Builder.Default
    private List<ParejaConvocada> parejas = new ArrayList<>();
    @Builder.Default
    private Map<String, Integer> puntosPorJugador = new HashMap<>(); // ✅ snapshot por username
}
