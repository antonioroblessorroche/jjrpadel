package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.*;
import com.jjrpadel.app.service.EntrenamientoService;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import java.util.stream.Collectors;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Route(value = "entrenamientos", layout = MainLayout.class)
@PageTitle("Entrenamientos")
@PermitAll
@RequiredArgsConstructor
public class EntrenamientosView extends VerticalLayout {

    private final EntrenamientoService entrenamientoService;
    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private final Grid<Entrenamiento> grid = new Grid<>(Entrenamiento.class, false);
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
        add(new H2("Entrenamientos"));

        // Crear nuevo (solo admin/capitán)
        Button btnNuevo = new Button("Nuevo entrenamiento", new Icon(VaadinIcon.PLUS));
        btnNuevo.addClickListener(e -> abrirDialogo(null));
        btnNuevo.setVisible(isAdmin() || isCapitan());
        add(new HorizontalLayout(btnNuevo));

        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        // ✅ Solo estas 3 columnas en la tabla
        grid.addColumn(e ->
                        (e.getFecha() == null ? "" : FECHA_FMT.format(e.getFecha())) + " " +
                                (e.getHora()  == null ? "" : HORA_FMT.format(e.getHora())))
                .setHeader("Fecha")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Entrenamiento::getLocalizacion)
                .setHeader("Club")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Entrenamiento::getEquipo)
                .setHeader("Equipo")
                .setAutoWidth(true)
                .setSortable(true);

        // ▶️ Detalles por fila (al hacer click)
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::crearDetalleEntrenamiento));

        grid.addItemClickListener(ev -> {
            Entrenamiento item = ev.getItem();
            boolean visible = grid.isDetailsVisible(item);
            grid.setDetailsVisible(item, !visible);
        });

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeightFull();
    }

    private void abrirDialogoAnadirJugadores(Entrenamiento e) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Añadir jugadores al entrenamiento");

        // Candidatos: usuarios del mismo equipo que NO estén ya inscritos
        var todosEquipo = usuarioService.findByEquipo(e.getEquipo());
        var yaInscritos = e.getInscripciones() == null
                ? java.util.Set.<String>of()
                : e.getInscripciones().stream().map(Inscripcion::getUsername).collect(Collectors.toSet());

        var candidatos = todosEquipo.stream()
                .filter(u -> !yaInscritos.contains(u.getUsername()))
                .collect(Collectors.toList());

        MultiSelectComboBox<Usuario> select = new MultiSelectComboBox<>("Jugadores del equipo " + e.getEquipo());
        select.setItems(candidatos);
        select.setItemLabelGenerator(u ->
                (ns(u.getNombre()) + " " + ns(u.getApellidos())).trim()
                        + " (" + (u.getPuntos() == null ? 0 : u.getPuntos()) + " pts) [" + u.getUsername() + "]"
        );
        select.setWidthFull();

        Button guardar = new Button("Añadir", ev -> {
            var seleccion = select.getSelectedItems();
            if (seleccion == null || seleccion.isEmpty()) {
                Notification.show("Selecciona al menos un jugador");
                return;
            }
            var usernames = seleccion.stream().map(Usuario::getUsername).toList();

            try {
                entrenamientoService.apuntarVarios(e, usernames);
                Notification.show("Jugadores añadidos");
                d.close();
                refreshManteniendoDetalles(e);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
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

    private VerticalLayout crearDetalleEntrenamiento(Entrenamiento e) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(true);
        wrapper.setSpacing(false);

        int count = (e.getInscripciones() == null) ? 0 : e.getInscripciones().size();
        H3 titulo = new H3("Apuntados (" + count + ")");
        UnorderedList lista = new UnorderedList();
        lista.getStyle().set("margin", "0");

        if (e.getInscripciones() != null && !e.getInscripciones().isEmpty()) {
            // Ordenar por fecha/hora ascendente (primero el que se apuntó antes)
            e.getInscripciones().stream()
                    .sorted(java.util.Comparator.comparing(Inscripcion::getFechaHora))
                    .forEachOrdered(ins -> {
                        var uOpt = usuarioService.findByUsername(ins.getUsername());
                        String nombreCompleto = uOpt
                                .map(u -> (nullSafe(u.getNombre()) + " " + nullSafe(u.getApellidos())).trim())
                                .orElse(ins.getUsername());
                        String when = ins.getFechaHora() != null
                                ? ins.getFechaHora().toString().replace('T', ' ')
                                : "";
                        lista.add(new ListItem(nombreCompleto + " — " + when));
                    });
        } else {
            lista.add(new ListItem(new Span("No hay jugadores apuntados.")));
        }

        // Acciones (las mismas que ya tenías)
        HorizontalLayout acciones = new HorizontalLayout();
        acciones.setSpacing(true);

        Button toggleApuntar;
        if (estaApuntado(e)) {
            toggleApuntar = new Button("Bajarme", click -> {
                desapuntarse(e);
                refreshManteniendoDetalles(e);
            });
        } else {
            toggleApuntar = new Button("Apuntarme", click -> {
                apuntarse(e);
                refreshManteniendoDetalles(e);
            });
        }
        toggleApuntar.setEnabled(puedeVer(e) && (isUser() || isCapitan() || isAdmin()));

        Button editar = new Button("Editar", new Icon(VaadinIcon.EDIT));
        editar.addClickListener(click -> abrirDialogo(e));
        editar.setEnabled(isAdmin() || (isCapitan() && mismoEquipo(e)));

        Button borrar = new Button("Borrar", new Icon(VaadinIcon.TRASH));
        borrar.getElement().setAttribute("theme", "error primary");
        borrar.addClickListener(click -> {
            if (isAdmin() || (isCapitan() && mismoEquipo(e))) {
                entrenamientoService.deleteById(e.getId());
                Notification.show("Entrenamiento eliminado");
                refresh();
            }
        });
        borrar.setEnabled(isAdmin() || (isCapitan() && mismoEquipo(e)));


        Button btnAnadir = new Button("Añadir jugadores", new Icon(VaadinIcon.USER_CHECK));
        btnAnadir.setEnabled(isAdmin() || (isCapitan() && mismoEquipo(e)));
        btnAnadir.addClickListener(ev -> abrirDialogoAnadirJugadores(e));

        acciones.add(toggleApuntar, editar, borrar, btnAnadir);

        wrapper.add(titulo, lista, acciones);
        return wrapper;
    }

    private void abrirDialogo(Entrenamiento existente) {
        Dialog d = new Dialog();
        d.setHeaderTitle(existente == null ? "Nuevo entrenamiento" : "Editar entrenamiento");

        DatePicker fecha = new DatePicker("Fecha");
        TimePicker  hora  = new TimePicker("Hora");
        TextField   local = new TextField("Club");
        IntegerField pistas = new IntegerField("Nº de pistas");
        pistas.setMin(1);
        Select<Equipo> equipo = new Select<>();
        equipo.setLabel("Equipo");
        equipo.setItems(Equipo.values());

        // Si es capitán, equipo bloqueado al suyo
        if (isCapitan()) {
            equipo.setValue(currentUser.getEquipo());
            equipo.setReadOnly(true);
        }

        if (existente != null) {
            if (existente.getFecha() != null) fecha.setValue(existente.getFecha());
            if (existente.getHora()  != null) hora.setValue(existente.getHora());
            local.setValue(Objects.toString(existente.getLocalizacion(), ""));
            pistas.setValue(existente.getPistas() == null ? 1 : existente.getPistas());
            equipo.setValue(existente.getEquipo());
        }

        Button guardar = new Button("Guardar", ev -> {
            if (fecha.getValue() == null || hora.getValue() == null || local.isEmpty()
                    || (equipo.getValue() == null) || pistas.getValue() == null || pistas.getValue() < 1) {
                Notification.show("Completa todos los campos correctamente");
                return;
            }
            Entrenamiento e = (existente != null) ? existente : new Entrenamiento();
            e.setFecha(fecha.getValue());
            e.setHora(hora.getValue());
            e.setLocalizacion(local.getValue());
            e.setPistas(pistas.getValue());
            e.setEquipo(isCapitan() ? currentUser.getEquipo() : equipo.getValue());

            entrenamientoService.save(e);
            d.close();
            refresh();
        });

        Button cancelar = new Button("Cancelar", ev -> d.close());

        FormLayout form = new FormLayout(fecha, hora, local, pistas, equipo);
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
            grid.setItems(entrenamientoService.findAll());
        } else {
            grid.setItems(entrenamientoService.findByEquipo(currentUser.getEquipo()));
        }
        // al refrescar, colapsamos detalles para evitar estados inconsistentes
        grid.getListDataView().getItems().forEach(it -> grid.setDetailsVisible(it, false));
    }

    private void refreshManteniendoDetalles(Entrenamiento e) {
        boolean abierto = grid.isDetailsVisible(e);
        refresh();
        // reabrir si seguía abierto (tras re-cargar colección)
        grid.getListDataView().getItems()
                .filter(it -> it.getId().equals(e.getId()))
                .findFirst()
                .ifPresent(it -> grid.setDetailsVisible(it, abierto));
    }

    // Helpers de permisos y utilidades
    private boolean puedeVer(Entrenamiento e) {
        if (isAdmin()) return true;
        if (isCapitan() || isUser()) return currentUser != null && e.getEquipo() == currentUser.getEquipo();
        return false;
    }
    private boolean mismoEquipo(Entrenamiento e) {
        return currentUser != null && e.getEquipo() == currentUser.getEquipo();
    }
    private boolean estaApuntado(Entrenamiento e) {
        if (currentUser == null || e.getInscripciones() == null) return false;
        return e.getInscripciones().stream()
                .anyMatch(i -> i.getUsername().equalsIgnoreCase(currentUser.getUsername()));
    }

    private void apuntarse(Entrenamiento e) {
        if (!puedeVer(e)) { Notification.show("No permitido"); return; }
        entrenamientoService.apuntar(e, currentUser.getUsername());
        Notification.show("Te has apuntado");
    }

    private void desapuntarse(Entrenamiento e) {
        entrenamientoService.desapuntar(e, currentUser.getUsername());
        Notification.show("Te has borrado del entrenamiento");
    }

    private boolean isAdmin()   { return currentUser != null && currentUser.getRol() == Role.ADMIN; }
    private boolean isCapitan() { return currentUser != null && currentUser.getRol() == Role.CAPITAN; }
    private boolean isUser()    { return currentUser != null && currentUser.getRol() == Role.USER; }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String ns(String s) { return s == null ? "" : s; }
}
