package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuarios")
@RolesAllowed("ADMIN")
public class UsuarioCrudView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final Grid<Usuario> grid = new Grid<>(Usuario.class, false);

    public UsuarioCrudView(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H2("Listado de Usuarios"));

        configureGrid();
        add(grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(u -> u.getNombre() + " " + u.getApellidos())
                .setHeader("Nombre completo")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Usuario::getPuntos)
                .setHeader("Puntos")
                .setAutoWidth(true)
                .setSortable(true);

        // ✅ Icono que redirige a vista de edición
        grid.addComponentColumn(usuario -> {
                    Button edit = new Button(new Icon(VaadinIcon.EDIT));
                    edit.addClickListener(e ->
                            edit.getUI().ifPresent(ui -> ui.navigate("usuarios/editar/" + usuario.getId()))
                    );
                    edit.addThemeName("tertiary-inline");
                    edit.getElement().setProperty("title", "Editar usuario");
                    return edit;
                }).setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeightFull();
    }

    private void updateGrid() {
        grid.setItems(usuarioService.findAll());
    }
}