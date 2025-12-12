package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Partido;
import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.service.PartidoService;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

import java.time.format.DateTimeFormatter;

@Route(value = "resultados-snp", layout = MainLayout.class)
@PageTitle("Resultados SNP")
@PermitAll
@RequiredArgsConstructor
public class ResultadosSnpListView extends VerticalLayout {

    private final PartidoService partidoService;
    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private final Grid<Partido> grid = new Grid<>(Partido.class, false);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA  = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setSizeFull();
        buildUI();
        refresh();
    }

    private void buildUI() {
        removeAll();
        add(new H2("Resultados SNP"));

        grid.addColumn(p -> (p.getFecha() == null ? "" : FECHA.format(p.getFecha())) + " " +
                           (p.getHora()  == null ? "" : HORA.format(p.getHora())))
            .setHeader("Fecha + hora").setAutoWidth(true).setSortable(true);
        grid.addColumn(Partido::getLocalizacion).setHeader("Club").setAutoWidth(true).setSortable(true);
        grid.addColumn(Partido::getEquipo).setHeader("Equipo").setAutoWidth(true).setSortable(true);
        grid.addColumn(p -> p.getSnpResultado() != null ? "Registrado" : "Pendiente")
            .setHeader("Estado").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeightFull();

        grid.addItemClickListener(ev ->
            ev.getSource().getUI().ifPresent(ui -> ui.navigate("resultados-snp/" + ev.getItem().getId()))
        );

        add(grid);
    }

    private void refresh() {
        var current = auth.getPrincipalName().flatMap(usuarioService::findByUsername).orElse(null);
        if (current == null) { grid.setItems(); return; }

        if (current.getRol() == Role.ADMIN) {
            grid.setItems(partidoService.findAll().stream().filter(Partido::isEsSnp).toList());
        } else {
            grid.setItems(partidoService.findByEquipo(current.getEquipo())
                    .stream().filter(Partido::isEsSnp).toList());
        }
    }
}
