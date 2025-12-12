// src/main/java/com/jjrpadel/app/service/PartidoService.java
package com.jjrpadel.app.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jjrpadel.app.model.Partido;
import com.jjrpadel.app.model.Equipo;
import com.jjrpadel.app.model.Inscripcion;
import com.jjrpadel.app.repository.PartidoRepository;

import java.time.LocalDateTime;
import java.util.*;
import com.jjrpadel.app.model.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartidoService {
    private final PartidoRepository repo;
    private final UsuarioService usuarioService;

    public List<Partido> findAll() { return repo.findAll(); }
    public Optional<Partido> findById(String id) { return repo.findById(id); }
    public List<Partido> findByEquipo(Equipo equipo) { return repo.findByEquipo(equipo); }
    public Partido save(Partido p) { return repo.save(p); }
    public void deleteById(String id) { repo.deleteById(id); }

    public Partido apuntarVarios(Partido p, List<String> usernames) {
        if (p.getInscripciones() == null) {
            p.setInscripciones(new java.util.ArrayList<>());
        }
        for (String username : usernames) {
            boolean ya = p.getInscripciones().stream()
                    .anyMatch(i -> i.getUsername().equalsIgnoreCase(username));
            if (!ya) {
                p.getInscripciones().add(
                        Inscripcion.builder()
                                .username(username)
                                .fechaHora(LocalDateTime.now())
                                .build()
                );
            }
        }
        // Mantén el orden por fecha/hora de inscripción
        p.getInscripciones().sort(Comparator.comparing(Inscripcion::getFechaHora));
        return repo.save(p);
    }

    public Partido apuntar(Partido p, String username) {
        if (p.getInscripciones() == null) p.setInscripciones(new java.util.ArrayList<>());
        boolean ya = p.getInscripciones().stream().anyMatch(i -> i.getUsername().equalsIgnoreCase(username));
        if (!ya) {
            p.getInscripciones().add(Inscripcion.builder()
                    .username(username)
                    .fechaHora(LocalDateTime.now())
                    .build());
            p.getInscripciones().sort(Comparator.comparing(Inscripcion::getFechaHora));
            p = repo.save(p);
        }
        return p;
    }

    public Partido desapuntar(Partido p, String username) {
        if (p.getInscripciones() != null) {
            p.getInscripciones().removeIf(i -> i.getUsername().equalsIgnoreCase(username));
            p = repo.save(p);
        }
        return p;
    }

    public Partido guardarConvocatoria(Partido p,
                                       List<String> usernamesSeleccionados,
                                       Map<String,Integer> puntosPorUser) {
        if (usernamesSeleccionados == null || usernamesSeleccionados.size() != 10) {
            throw new IllegalArgumentException("Deben seleccionarse exactamente 10 jugadores");
        }

        // Orden por puntos (desc)
        List<String> sorted = new ArrayList<>(usernamesSeleccionados);
        sorted.sort((a,b) -> Integer.compare(
                puntosPorUser.getOrDefault(b, 0),
                puntosPorUser.getOrDefault(a, 0))
        );

        List<ParejaConvocada> parejas = new ArrayList<>();
        for (int i = 0; i < 10; i += 2) {
            String u1 = sorted.get(i);
            String u2 = sorted.get(i+1);
            int pj1 = puntosPorUser.getOrDefault(u1, 0);
            int pj2 = puntosPorUser.getOrDefault(u2, 0);
            parejas.add(ParejaConvocada.builder()
                    .jugador1(u1)
                    .jugador2(u2)
                    .puntosJ1(pj1)
                    .puntosJ2(pj2)
                    .puntosTotal(pj1 + pj2)
                    .build());
        }

        // Ordenar parejas por puntosTotal desc
        parejas.sort((x,y) -> Integer.compare(y.getPuntosTotal(), x.getPuntosTotal()));

        Convocatoria c = Convocatoria.builder()
                .creadaEn(LocalDateTime.now())
                .parejas(parejas)
                .puntosPorJugador(new HashMap<>(puntosPorUser))
                .build();

        p.setConvocatoria(c);
        return repo.save(p);
    }

    public Partido guardarConvocatoriaManual(Partido p, List<ParejaConvocada> parejas) {
        if (parejas == null || parejas.size() != 5) {
            throw new IllegalArgumentException("Debes proporcionar exactamente 5 parejas");
        }
        // Ordenar por puntosTotal desc
        parejas.sort(Comparator.comparing(ParejaConvocada::getPuntosTotal).reversed());

        Convocatoria c = Convocatoria.builder()
                .creadaEn(LocalDateTime.now())
                .parejas(new ArrayList<>(parejas))
                .build();

        p.setConvocatoria(c);
        return repo.save(p);
    }

    public Partido initSnpResultadoDesdeConvocatoria(Partido p) {
        if (p.getConvocatoria() == null
                || p.getConvocatoria().getParejas() == null
                || p.getConvocatoria().getParejas().isEmpty()) {
            throw new IllegalStateException("No hay convocatoria para este partido");
        }
        if (p.getSnpResultado() != null
                && p.getSnpResultado().getResultados() != null
                && !p.getSnpResultado().getResultados().isEmpty()) {
            return p; // ya inicializado
        }

        SnpResultado res = new SnpResultado();
        res.setSomosLocales(Boolean.TRUE); // por defecto; editable en UI
        res.setRegistradoEn(java.time.LocalDateTime.now());

        java.util.List<ResultadoPareja> lista = new java.util.ArrayList<>();
        for (ParejaConvocada pareja : p.getConvocatoria().getParejas()) {

            Usuario u1 = usuarioService.findByUsername(pareja.getJugador1()).orElse(null);
            Usuario u2 = usuarioService.findByUsername(pareja.getJugador2()).orElse(null);

            String n1 = (u1 == null)
                    ? pareja.getJugador1()
                    : (safe(u1.getNombre()) + " " + safe(u1.getApellidos())).trim();

            String n2 = (u2 == null)
                    ? pareja.getJugador2()
                    : (safe(u2.getNombre()) + " " + safe(u2.getApellidos())).trim();

            ResultadoPareja rp = ResultadoPareja.builder()
                    .jugador1(pareja.getJugador1())
                    .jugador2(pareja.getJugador2())
                    .puntosJ1(nz(pareja.getPuntosJ1()))
                    .puntosJ2(nz(pareja.getPuntosJ2()))
                    .totalNuestro(nz(pareja.getPuntosJ1()) + nz(pareja.getPuntosJ2()))
                    .nuestros1Nombre(n1)  // nombres visibles editables
                    .nuestros2Nombre(n2)
                    .puntosRival1(0)
                    .puntosRival2(0)
                    .totalRival(0)
                    .jugadoTercerSet(false)
                    .s1N(0).s1R(0).s2N(0).s2R(0)
                    .build();

            lista.add(rp);
        }
        res.setResultados(lista);
        p.setSnpResultado(res);
        return repo.save(p);
    }

    // Helpers internos del servicio
    private static String safe(String s) { return s == null ? "" : s; }

    public Partido guardarSnpResultado(Partido p, SnpResultado snp) {
        if (snp.getResultados() != null) {
            for (var r : snp.getResultados()) {
                // Totales rivales / nuestros por puntos (por seguridad)
                int pr1 = nz(r.getPuntosRival1());
                int pr2 = nz(r.getPuntosRival2());
                r.setTotalRival(pr1 + pr2);

                int pj1 = nz(r.getPuntosJ1());
                int pj2 = nz(r.getPuntosJ2());
                r.setTotalNuestro(pj1 + pj2);

                // Normaliza tercer set
                boolean t = Boolean.TRUE.equals(r.getJugadoTercerSet());
                if (!t) { r.setS3N(null); r.setS3R(null); }

                // Recalcula sets ganados
                int gNos = 0, gRiv = 0;
                if (r.getS1N() != null && r.getS1R() != null) {
                    if (r.getS1N() > r.getS1R()) gNos++; else if (r.getS1R() > r.getS1N()) gRiv++;
                }
                if (r.getS2N() != null && r.getS2R() != null) {
                    if (r.getS2N() > r.getS2R()) gNos++; else if (r.getS2R() > r.getS2N()) gRiv++;
                }
                if (t && r.getS3N() != null && r.getS3R() != null) {
                    if (r.getS3N() > r.getS3R()) gNos++; else if (r.getS3R() > r.getS3N()) gRiv++;
                }
                r.setSetsGanadosNos(gNos);
                r.setSetsGanadosRival(gRiv);
            }
        }
        snp.setRegistradoEn(java.time.LocalDateTime.now());
        p.setSnpResultado(snp);
        return repo.save(p);
    }

    private static int nz(Integer i) { return i == null ? 0 : i; }

}
