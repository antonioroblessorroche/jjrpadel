package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;

@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuarios")
@PermitAll
public class UsuarioCrudView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;
    private final Grid<Usuario> grid = new Grid<>(Usuario.class, false);

    private boolean isAdmin = false;
    private boolean isCapitan = false;

    public UsuarioCrudView(UsuarioService usuarioService, AuthenticationContext auth) {
        this.usuarioService = usuarioService;
        this.auth = auth;

        setSizeFull();
        setPadding(true);

        // Usuario actual y permisos
        var currentOpt = auth.getPrincipalName().flatMap(usuarioService::findByUsername);
        var current = currentOpt.orElse(null);
        if (current != null) {
            isAdmin = current.getRol() == Role.ADMIN;
            isCapitan = current.getRol() == Role.CAPITAN;
        }

        // Barra superior
        H2 titulo = new H2("Plantilla");
        Button nuevo = new Button("Añadir jugador", e -> getUI().ifPresent(ui -> ui.navigate("admin/alta-usuario")));
        nuevo.addThemeName("primary");
        nuevo.setVisible(isAdmin || isCapitan); // solo capitán/admin

        HorizontalLayout barra = new HorizontalLayout(titulo, nuevo);
        barra.setWidthFull();
        barra.setAlignItems(Alignment.CENTER);
        barra.expand(titulo);
        add(barra);

        configureGrid(current);
        add(grid);

        updateGrid(current);
    }

    private void configureGrid(Usuario current) {
        grid.addColumn(u -> (nullSafe(u.getNombre()) + " " + nullSafe(u.getApellidos())).trim())
                .setHeader("Nombre completo")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(u -> u.getPuntos() == null ? 0 : u.getPuntos())
                .setHeader("Puntos")
                .setAutoWidth(true)
                .setSortable(true);

        // Columna editar solo para admin/capitán
        var editCol = grid.addComponentColumn(usuario -> {
                    Button edit = new Button(new Icon(VaadinIcon.EDIT));
                    edit.addThemeName("tertiary-inline");
                    edit.getElement().setProperty("title", "Editar usuario");
                    edit.addClickListener(e ->
                            edit.getUI().ifPresent(ui -> ui.navigate("usuarios/editar/" + usuario.getId()))
                    );
                    return edit;
                }).setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
        editCol.setVisible(isAdmin || isCapitan);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeightFull();
    }

    private void updateGrid(Usuario current) {
        if (current == null) {
            grid.setItems();
            return;
        }
        if (isAdmin) {
            grid.setItems(usuarioService.findAll());
        } else {
            // USER y CAPITAN: solo su equipo
            grid.setItems(usuarioService.findByEquipo(current.getEquipo()));
        }
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}