package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.*;
import com.jjrpadel.app.service.PartidoService;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "partidos/convocar", layout = MainLayout.class)
@PageTitle("Convocar partido")
@RolesAllowed({"ADMIN","CAPITAN"})
@RequiredArgsConstructor
public class ConvocarPartidoView extends VerticalLayout implements HasUrlParameter<String> {

    private final PartidoService partidoService;
    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private Partido partido;

    private boolean internalUpdate = false;

    private final MultiSelectComboBox<String> inscritosSelect = new MultiSelectComboBox<>("Selecciona 10 jugadores inscritos");
    private final Button continuar = new Button("Continuar");
    private final Button guardar = new Button("Guardar convocatoria");
    private final Button cancelar = new Button("Cancelar");

    // 5 parejas (cada una: 2 combos)
    private final List<ComboBox<String>> parejaA = new ArrayList<>();
    private final List<ComboBox<String>> parejaB = new ArrayList<>();

    // Cache de usuarios (username -> Usuario) y puntos
    private final Map<String, Usuario> usuariosByUsername = new HashMap<>();
    private final Map<String, Integer> puntosByUsername = new HashMap<>();

    @Override
    public void setParameter(BeforeEvent event, String partidoId) {
        partido = partidoService.findById(partidoId).orElse(null);
        if (partido == null) {
            Notification.show("Partido no encontrado");
            getUI().ifPresent(ui -> ui.navigate("partidos"));
            return;
        }

        setWidthFull();
        setMaxWidth("900px");
        buildStep1();
    }

    private void buildStep1() {
        removeAll();

        // Inscritos (usernames)
        List<String> inscritosUsernames = partido.getInscripciones() == null ? List.of()
                : partido.getInscripciones().stream().map(Inscripcion::getUsername).toList();

        // Cargamos usuarios y puntos
        usuariosByUsername.clear();
        puntosByUsername.clear();
        for (String u : inscritosUsernames) {
            usuarioService.findByUsername(u).ifPresent(user -> {
                usuariosByUsername.put(u, user);
                puntosByUsername.put(u, user.getPuntos() == null ? 0 : user.getPuntos());
            });
        }

        // MultiSelect con etiquetas informativas
        inscritosSelect.setItems(inscritosUsernames);
        inscritosSelect.setItemLabelGenerator(u -> {
            Usuario usr = usuariosByUsername.get(u);
            if (usr == null) return u;
            String nombre = safe(usr.getNombre()) + " " + safe(usr.getApellidos());
            int pts = usr.getPuntos() == null ? 0 : usr.getPuntos();
            return (nombre.trim().isEmpty() ? usr.getUsername() : nombre.trim()) + " (" + pts + " pts) [" + usr.getUsername() + "]";
        });
        inscritosSelect.setWidthFull();
        inscritosSelect.setHelperText("Debes seleccionar exactamente 10 jugadores");

        continuar.addClickListener(e -> {
            Set<String> sel = inscritosSelect.getSelectedItems();
            if (sel.size() != 10) {
                Notification.show("Selecciona exactamente 10 jugadores");
                return;
            }
            buildStep2(new ArrayList<>(sel));
        });
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("partidos")));

        add(new H2("Convocar partido"),
                new Paragraph("Partido: " + (partido.getFecha() != null ? partido.getFecha() : "") +
                        " " + (partido.getHora() != null ? partido.getHora() : "") +
                        " · " + partido.getLocalizacion() +
                        " · Equipo " + partido.getEquipo()),
                inscritosSelect,
                new HorizontalLayout(continuar, cancelar)
        );
    }

    private void buildStep2(List<String> seleccion10) {
        removeAll();

        // Prepara 5 parejas -> 10 combos en total
        parejaA.clear(); parejaB.clear();
        FormLayout formParejas = new FormLayout();
        formParejas.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        // Fuente de opciones inicial: los 10 seleccionados
        List<String> opciones = new ArrayList<>(seleccion10);

        for (int i = 0; i < 5; i++) {
            ComboBox<String> c1 = crearComboJugador("Pareja " + (i + 1) + " - Jugador 1", opciones);
            ComboBox<String> c2 = crearComboJugador("Pareja " + (i + 1) + " - Jugador 2", opciones);
            int idx = i;

            // Evitar duplicados entre todos los combos
            c1.addValueChangeListener(ev -> actualizarOpciones(idx));
            c2.addValueChangeListener(ev -> actualizarOpciones(idx));

            parejaA.add(c1);
            parejaB.add(c2);

            formParejas.add(c1, c2);
        }

        // Preselección rápida (opcional): no empareja, solo facilita
        // — puedes omitirlo si prefieres empezar en blanco.

        guardar.addClickListener(e -> {
            // Validaciones
            Set<String> usados = new HashSet<>();
            List<ParejaConvocada> parejas = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                String u1 = parejaA.get(i).getValue();
                String u2 = parejaB.get(i).getValue();
                if (u1 == null || u2 == null) {
                    Notification.show("Completa todas las parejas (faltan jugadores)");
                    return;
                }
                if (u1.equals(u2)) {
                    Notification.show("Una pareja no puede tener el mismo jugador dos veces");
                    return;
                }
                if (usados.contains(u1) || usados.contains(u2)) {
                    Notification.show("Un jugador no puede estar en dos parejas");
                    return;
                }
                usados.add(u1); usados.add(u2);

                int p1 = puntosByUsername.getOrDefault(u1, 0);
                int p2 = puntosByUsername.getOrDefault(u2, 0);

                parejas.add(ParejaConvocada.builder()
                        .jugador1(u1)
                        .jugador2(u2)
                        .puntosJ1(p1)
                        .puntosJ2(p2)
                        .puntosTotal(p1 + p2)
                        .build());
            }

            if (usados.size() != 10) {
                Notification.show("Debes usar exactamente a los 10 seleccionados");
                return;
            }

            try {
                partidoService.guardarConvocatoriaManual(partido, parejas);
                Notification.show("Convocatoria guardada");
                getUI().ifPresent(ui -> ui.navigate("partidos"));
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });

        Button volver = new Button("Volver", e -> buildStep1());

        add(new H2("Formar parejas (elige manualmente)"),
                new Paragraph("Selecciona exactamente 10 jugadores y forma 5 parejas. "
                        + "Al guardar, se ordenarán por puntos totales (snapshot)"),
                formParejas,
                new HorizontalLayout(guardar, volver, cancelar)
        );

        // Inicializa opciones coherentes
        actualizarOpciones(-1);
    }

    private ComboBox<String> crearComboJugador(String label, List<String> opciones) {
        ComboBox<String> cb = new ComboBox<>(label);
        cb.setItems(opciones);
        cb.setItemLabelGenerator(u -> {
            Usuario usr = usuariosByUsername.get(u);
            if (usr == null) return u;
            String nombre = safe(usr.getNombre()) + " " + safe(usr.getApellidos());
            int pts = usr.getPuntos() == null ? 0 : usr.getPuntos();
            return (nombre.trim().isEmpty() ? usr.getUsername() : nombre.trim())
                    + " (" + pts + " pts) [" + usr.getUsername() + "]";
        });
        cb.setAllowCustomValue(false);
        cb.setClearButtonVisible(true);
        cb.setWidthFull();
        return cb;
    }

    /**
     * Recalcula el conjunto de opciones disponibles para todos los combos
     * evitando que un mismo username se repita en más de un combo.
     */
    private void actualizarOpciones(int changedIndex) {
        if (internalUpdate) return;
        internalUpdate = true;
        try {
            // 1) Recolecta los valores actuales (para reponerlos luego)
            String[] currentA = new String[5];
            String[] currentB = new String[5];
            for (int i = 0; i < 5; i++) {
                currentA[i] = parejaA.get(i).getValue();
                currentB[i] = parejaB.get(i).getValue();
            }

            // 2) Conjunto de usados (sin nulls)
            Set<String> usados = new HashSet<>();
            for (var c : parejaA) if (c.getValue() != null) usados.add(c.getValue());
            for (var c : parejaB) if (c.getValue() != null) usados.add(c.getValue());

            // Base = los 10 seleccionados en el paso 1
            List<String> base = new ArrayList<>(inscritosSelect.getSelectedItems());

            // 3) Recalcula items para cada combo preservando SU propio valor
            for (int i = 0; i < 5; i++) {
                // Para A[i]
                {
                    var c = parejaA.get(i);
                    Set<String> usadosMenosEste = new HashSet<>(usados);
                    if (currentA[i] != null) usadosMenosEste.remove(currentA[i]); // <- clave
                    List<String> items = base.stream()
                            .filter(u -> !usadosMenosEste.contains(u))
                            .collect(Collectors.toList());
                    c.setItems(items);
                }
                // Para B[i]
                {
                    var c = parejaB.get(i);
                    Set<String> usadosMenosEste = new HashSet<>(usados);
                    if (currentB[i] != null) usadosMenosEste.remove(currentB[i]); // <- clave
                    List<String> items = base.stream()
                            .filter(u -> !usadosMenosEste.contains(u))
                            .collect(Collectors.toList());
                    c.setItems(items);
                }
            }

            // 4) Restaura los valores si siguen disponibles
            for (int i = 0; i < 5; i++) {
                int finalI = i;
                if (currentA[i] != null && parejaA.get(i).getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                        .anyMatch(opt -> Objects.equals(opt, currentA[finalI]))) {
                    parejaA.get(i).setValue(currentA[i]);
                }
                if (currentB[i] != null && parejaB.get(i).getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                        .anyMatch(opt -> Objects.equals(opt, currentB[finalI]))) {
                    parejaB.get(i).setValue(currentB[i]);
                }
            }
        } finally {
            internalUpdate = false;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
