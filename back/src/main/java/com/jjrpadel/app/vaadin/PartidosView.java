// src/main/java/com/jjrpadel/app/vaadin/PartidosView.java
package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.*;
import com.jjrpadel.app.service.PartidoService;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import java.util.stream.Collectors;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Route(value = "partidos", layout = MainLayout.class)
@PageTitle("Partidos")
@PermitAll
@RequiredArgsConstructor
public class PartidosView extends VerticalLayout {

    private final PartidoService partidoService;
    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private final Grid<Partido> grid = new Grid<>(Partido.class, false);
    private Usuario currentUser;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT  = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        String username = auth.getPrincipalName().orElse(null);
        currentUser = (username == null) ? null : usuarioService.findByUsername(username).orElse(null);
        buildUI();
        refresh();
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        add(new H2("Partidos"));

        Button btnNuevo = new Button("Nuevo partido", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClickListener(e -> abrirDialogo(null));
        btnNuevo.setVisible(isAdmin() || isCapitan());
        add(new HorizontalLayout(btnNuevo));

        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.addColumn(p ->
                (p.getFecha() == null ? "" : FECHA_FMT.format(p.getFecha())) + " " +
                (p.getHora()  == null ? "" : HORA_FMT.format(p.getHora())))
            .setHeader("Fecha + hora").setAutoWidth(true).setSortable(true);

        grid.addColumn(Partido::getLocalizacion)
            .setHeader("Club").setAutoWidth(true).setSortable(true);

        grid.addColumn(Partido::getEquipo)
            .setHeader("Equipo").setAutoWidth(true).setSortable(true);

        grid.addColumn(p -> p.isEsSnp() ? "SNP" : "-")
            .setHeader("Es SNP").setAutoWidth(true);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::crearDetallePartido));
        grid.addItemClickListener(ev -> {
            Partido item = ev.getItem();
            grid.setDetailsVisible(item, !grid.isDetailsVisible(item));
        });

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeightFull();
    }

    private void abrirDialogoAnadirJugadores(Partido p) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Añadir jugadores al partido");

        // Candidatos: todos los usuarios del mismo equipo que NO estén ya inscritos
        var todosEquipo = usuarioService.findByEquipo(p.getEquipo());
        var yaInscritos = p.getInscripciones() == null
                ? java.util.Set.<String>of()
                : p.getInscripciones().stream().map(Inscripcion::getUsername).collect(Collectors.toSet());

        var candidatos = todosEquipo.stream()
                .filter(u -> !yaInscritos.contains(u.getUsername()))
                .collect(Collectors.toList());

        MultiSelectComboBox<Usuario> select = new MultiSelectComboBox<>("Jugadores del equipo " + p.getEquipo());
        select.setItems(candidatos);
        select.setItemLabelGenerator(u ->
                (ns(u.getNombre()) + " " + ns(u.getApellidos())).trim()
                        + " (" + (u.getPuntos() == null ? 0 : u.getPuntos()) + " pts)"
        );
        select.setWidthFull();

        Button guardar = new Button("Añadir", ev -> {
            var seleccion = select.getSelectedItems();
            if (seleccion == null || seleccion.isEmpty()) {
                Notification.show("Selecciona al menos un jugador");
                return;
            }
            var usernames = seleccion.stream().map(Usuario::getUsername).toList();
            partidoService.apuntarVarios(p, usernames);
            Notification.show("Jugadores añadidos");
            d.close();
            refreshManteniendoDetalles(p);
        });

        Button cancelar = new Button("Cancelar", ev -> d.close());

        FormLayout form = new FormLayout(select);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        d.add(form, new HorizontalLayout(guardar, cancelar));
        d.open();
    }

    private VerticalLayout crearDetallePartido(Partido p) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(true);
        wrapper.setSpacing(false);

        int count = (p.getInscripciones() == null) ? 0 : p.getInscripciones().size();
        H3 titulo = new H3("Apuntados (" + count + ")");
        UnorderedList lista = new UnorderedList();
        lista.getStyle().set("margin", "0");

        if (p.getInscripciones() != null && !p.getInscripciones().isEmpty()) {
            p.getInscripciones().forEach(ins -> {
                var uOpt = usuarioService.findByUsername(ins.getUsername());
                String nombre = uOpt
                        .map(u -> (ns(u.getNombre()) + " " + ns(u.getApellidos())).trim())
                        .orElse(ins.getUsername());
                String when = ins.getFechaHora() != null
                        ? ins.getFechaHora().toString().replace('T', ' ')
                        : "";
                lista.add(new ListItem(nombre + " — " + when));
            });
        } else {
            lista.add(new ListItem(new Span("No hay jugadores apuntados.")));
        }

        // Acciones
        HorizontalLayout acciones = new HorizontalLayout();
        acciones.setSpacing(true);



        // Apuntarme / Bajarme
        Button toggleApuntar = estaApuntado(p)
                ? new Button("Bajarme", e -> { desapuntarse(p); refreshManteniendoDetalles(p); })
                : new Button("Apuntarme", e -> { apuntarse(p); refreshManteniendoDetalles(p); });
        toggleApuntar.setEnabled(puedeVer(p) && (isUser() || isCapitan() || isAdmin()));

        // Editar / Borrar
        Button editar = new Button("Editar", new Icon(VaadinIcon.EDIT));
        editar.addClickListener(e -> abrirDialogo(p));
        editar.setEnabled(isAdmin() || (isCapitan() && mismoEquipo(p)));

        Button borrar = new Button("Borrar", new Icon(VaadinIcon.TRASH));
        borrar.getElement().setAttribute("theme", "error primary");
        borrar.addClickListener(e -> {
            if (isAdmin() || (isCapitan() && mismoEquipo(p))) {
                partidoService.deleteById(p.getId());
                Notification.show("Partido eliminado");
                refresh();
            }
        });
        borrar.setEnabled(isAdmin() || (isCapitan() && mismoEquipo(p)));

        // ✅ Botón de navegación a ConvocarPartidoView
        boolean hayConvocatoria = p.getConvocatoria() != null
                && p.getConvocatoria().getParejas() != null
                && !p.getConvocatoria().getParejas().isEmpty();

        Button btnConvocar = new Button(hayConvocatoria ? "Gestionar convocatoria" : "Crear convocatoria",
                new Icon(VaadinIcon.USERS));
        btnConvocar.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("partidos/convocar/" + p.getId()))
        );
        btnConvocar.setEnabled(puedeGestionarConvocatoria(p) && (hayConvocatoria || tieneMinimoInscritos(p, 10)));

        Button btnAnadir = new Button("Añadir jugadores", new Icon(VaadinIcon.USER_CHECK));
        btnAnadir.setEnabled(isAdmin() || (isCapitan() && mismoEquipo(p)));
        btnAnadir.addClickListener(e -> abrirDialogoAnadirJugadores(p));

        Button resultadosBtn = new Button("Resultados SNP", new Icon(VaadinIcon.TROPHY));
        resultadosBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("resultados-snp/" + p.getId())));
        resultadosBtn.setVisible(p.isEsSnp());

        acciones.add(toggleApuntar, editar, borrar, btnConvocar, btnAnadir, resultadosBtn);

        // Mostrar convocatoria si existe
        if (hayConvocatoria) {

            H3 convocadosTitulo = new H3("Convocados (parejas ordenadas por puntos)");
            UnorderedList parejasUl = new UnorderedList();
            int idx = 1;
            for (var pareja : p.getConvocatoria().getParejas()) {
                var u1 = usuarioService.findByUsername(pareja.getJugador1()).orElse(null);
                var u2 = usuarioService.findByUsername(pareja.getJugador2()).orElse(null);
                String label1 = (u1 == null ? pareja.getJugador1()
                        : (ns(u1.getNombre()) + " " + ns(u1.getApellidos())).trim())
                        + " (" + (pareja.getPuntosJ1() == null ? 0 : pareja.getPuntosJ1()) + ")";
                String label2 = (u2 == null ? pareja.getJugador2()
                        : (ns(u2.getNombre()) + " " + ns(u2.getApellidos())).trim())
                        + " (" + (pareja.getPuntosJ2() == null ? 0 : pareja.getPuntosJ2()) + ")";
                parejasUl.add(new ListItem((idx++) + ") " + label1 + "  +  " + label2 +
                        "  = " + (pareja.getPuntosTotal() == null
                        ? (val(pareja.getPuntosJ1()) + val(pareja.getPuntosJ2()))
                        : pareja.getPuntosTotal()) + " pts"));
            }
            wrapper.add(titulo, lista, acciones, convocadosTitulo, parejasUl);
        } else {
            wrapper.add(titulo, lista, acciones);
        }

        return wrapper;
    }

    // Helpers nuevos
    private boolean puedeGestionarConvocatoria(Partido p) {
        return isAdmin() || (isCapitan() && mismoEquipo(p));
    }
    private boolean tieneMinimoInscritos(Partido p, int min) {
        int n = (p.getInscripciones() == null) ? 0 : p.getInscripciones().size();
        return n >= min;
    }

    private void abrirDialogo(Partido existente) {
        Dialog d = new Dialog();
        d.setHeaderTitle(existente == null ? "Nuevo partido" : "Editar partido");

        DatePicker fecha = new DatePicker("Fecha");
        TimePicker  hora  = new TimePicker("Hora");
        TextField   club  = new TextField("Club");
        Select<Equipo> equipo = new Select<>();
        equipo.setLabel("Equipo");
        equipo.setItems(Equipo.values());

        Checkbox esSnp = new Checkbox("Es SNP");

        if (isCapitan()) {
            equipo.setValue(currentUser.getEquipo());
            equipo.setReadOnly(true);
        }

        if (existente != null) {
            if (existente.getFecha() != null) fecha.setValue(existente.getFecha());
            if (existente.getHora()  != null) hora.setValue(existente.getHora());
            club.setValue(Objects.toString(existente.getLocalizacion(), ""));
            equipo.setValue(existente.getEquipo());
            esSnp.setValue(existente.isEsSnp());
        }

        Button guardar = new Button("Guardar", ev -> {
            if (fecha.getValue() == null || hora.getValue() == null || club.isEmpty()
                || equipo.getValue() == null) {
                Notification.show("Completa todos los campos");
                return;
            }
            Partido p = (existente != null) ? existente : new Partido();
            p.setFecha(fecha.getValue());
            p.setHora(hora.getValue());
            p.setLocalizacion(club.getValue());
            p.setEquipo(isCapitan() ? currentUser.getEquipo() : equipo.getValue());
            p.setEsSnp(esSnp.getValue());

            partidoService.save(p);
            d.close();
            refresh();
        });

        Button cancelar = new Button("Cancelar", ev -> d.close());

        FormLayout form = new FormLayout(fecha, hora, club, equipo, esSnp);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("700px", 2)
        );

        d.add(form, new HorizontalLayout(guardar, cancelar));
        d.open();
    }

    private void refresh() {
        if (currentUser == null) { grid.setItems(); return; }
        if (isAdmin()) {
            grid.setItems(partidoService.findAll());
        } else {
            grid.setItems(partidoService.findByEquipo(currentUser.getEquipo()));
        }
        grid.getListDataView().getItems().forEach(it -> grid.setDetailsVisible(it, false));
    }

    private void refreshManteniendoDetalles(Partido p) {
        boolean abierto = grid.isDetailsVisible(p);
        refresh();
        grid.getListDataView().getItems()
            .filter(it -> it.getId().equals(p.getId()))
            .findFirst()
            .ifPresent(it -> grid.setDetailsVisible(it, abierto));
    }

    // Helpers
    private boolean puedeVer(Partido p) {
        if (isAdmin()) return true;
        if (isCapitan() || isUser()) return currentUser != null && p.getEquipo() == currentUser.getEquipo();
        return false;
    }
    private boolean mismoEquipo(Partido p) {
        return currentUser != null && p.getEquipo() == currentUser.getEquipo();
    }
    private boolean estaApuntado(Partido p) {
        if (currentUser == null || p.getInscripciones() == null) return false;
        return p.getInscripciones().stream()
                .anyMatch(i -> i.getUsername().equalsIgnoreCase(currentUser.getUsername()));
    }
    private void apuntarse(Partido p) {
        if (!puedeVer(p)) { Notification.show("No permitido"); return; }
        partidoService.apuntar(p, currentUser.getUsername());
        Notification.show("Te has apuntado");
    }
    private void desapuntarse(Partido p) {
        partidoService.desapuntar(p, currentUser.getUsername());
        Notification.show("Te has borrado del partido");
    }

    private boolean isAdmin()   { return currentUser != null && currentUser.getRol() == Role.ADMIN; }
    private boolean isCapitan() { return currentUser != null && currentUser.getRol() == Role.CAPITAN; }
    private boolean isUser()    { return currentUser != null && currentUser.getRol() == Role.USER; }

    private static String ns(String s) { return s == null ? "" : s; }
    private static int val(Integer i) { return i == null ? 0 : i; }
}
