package com.jjrpadel.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SnpResultado {
    private Boolean somosLocales; // true = local, false = visitante
    private LocalDateTime registradoEn;

    @Builder.Default
    private List<ResultadoPareja> resultados = new ArrayList<>();
}
