// src/main/java/com/jjrpadel/app/vaadin/ClasificacionView.java
package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.ClasificacionEntry;
import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.ClasificacionService;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;     // ✅ NUEVO
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "clasificacion", layout = MainLayout.class)
@PageTitle("Clasificación")
@PermitAll
@RequiredArgsConstructor
public class ClasificacionView extends VerticalLayout {

    private final ClasificacionService clasificacionService;
    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private final Grid<ClasificacionEntry> grid = new Grid<>(ClasificacionEntry.class, false);
    private final ComboBox<String> grupoSelect = new ComboBox<>("Grupo");
    private final TextField grupoNuevo = new TextField("Grupo para guardar (opcional)");

    // ✅ NUEVO: Área de pegado de CSV y acciones
    private final TextArea csvArea = new TextArea("Pega aquí el CSV");
    private final Button btnPrevisualizar = new Button("Previsualizar CSV");
    private final Button btnGuardar = new Button("Guardar en grupo");

    private boolean puedeEditar = false;
    private List<ClasificacionEntry> previsualizacion = List.of(); // staging

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setSizeFull();
        buildUI();
        initPermisos();
        loadGrupos();
        refresh();
    }

    private void buildUI() {
        add(new H2("Clasificación"));

        // Top bar
        grupoSelect.setPlaceholder("Selecciona grupo");
        grupoSelect.addValueChangeListener(e -> refresh());

        HorizontalLayout top = new HorizontalLayout(
                new Span("Grupo a visualizar: "), grupoSelect
        );
        top.setAlignItems(Alignment.END);
        add(top);

        // ✅ NUEVO: Bloque de pegado CSV (solo cap/admin)
        csvArea.setPlaceholder("Encabezados y columnas como en tu plantilla.\n"
                + "Separador: tabulador, coma o punto y coma.");
        csvArea.setWidthFull();
        csvArea.setHeight("220px");

        btnPrevisualizar.addClickListener(e -> {
            try {
                previsualizacion = parseClasificacionCsv(csvArea.getValue());
                if (previsualizacion.isEmpty()) {
                    Notification.show("No se encontraron filas válidas.");
                    return;
                }
                grid.setItems(previsualizacion);
                Notification.show("Previsualización cargada (" + previsualizacion.size() + " filas)");
            } catch (Exception ex) {
                Notification.show("Error al procesar CSV: " + ex.getMessage());
            }
        });

        btnGuardar.addClickListener(e -> {
            if (previsualizacion == null || previsualizacion.isEmpty()) {
                Notification.show("Previsualiza primero el CSV");
                return;
            }
            String grupoDestino = (grupoNuevo.getValue() != null && !grupoNuevo.getValue().isBlank())
                    ? grupoNuevo.getValue().trim()
                    : grupoSelect.getValue();
            if (grupoDestino == null || grupoDestino.isBlank()) {
                Notification.show("Indica un grupo (elige uno o escribe uno nuevo)");
                return;
            }
            try {
                clasificacionService.replaceGrupo(grupoDestino, previsualizacion);
                Notification.show("Clasificación guardada en " + grupoDestino);
                if (!Objects.equals(grupoSelect.getValue(), grupoDestino)) {
                    grupoSelect.setValue(grupoDestino);
                }
                loadGrupos();
                refresh();
            } catch (Exception ex) {
                Notification.show("No se pudo guardar: " + ex.getMessage());
            }
        });

        // Lo mostramos dentro de un acordeón "Solo capitanes"
        VerticalLayout pasteBlock = new VerticalLayout(
                new Span("Solo capitanes y administradores pueden actualizar."),
                grupoNuevo, csvArea,
                new HorizontalLayout(btnPrevisualizar, btnGuardar)
        );
        pasteBlock.setPadding(false);
        Details uploader = new Details("Actualizar desde CSV (capitanes/admin)", pasteBlock);
        uploader.setOpened(false);
        add(uploader);

        configureGrid();
        add(grid);
        expand(grid);
    }

    private void initPermisos() {
        Usuario current = auth.getPrincipalName()
                .flatMap(usuarioService::findByUsername)
                .orElse(null);
        puedeEditar = current != null && (current.getRol() == Role.ADMIN || current.getRol() == Role.CAPITAN);

        // ocultar bloque CSV si no tiene permisos
        getChildren().filter(c -> c instanceof Details).findFirst()
                .ifPresent(c -> c.setVisible(puedeEditar));
        grupoNuevo.setVisible(puedeEditar);
        btnGuardar.setVisible(puedeEditar);
        btnPrevisualizar.setVisible(puedeEditar);
        csvArea.setVisible(puedeEditar);
    }

    private void configureGrid() {
        grid.addColumn(ClasificacionEntry::getEquipo).setHeader("Equipos").setAutoWidth(true).setSortable(true);

        grid.addColumn(c -> nz(c.getPuntos())).setHeader("Puntos").setSortable(true);
        grid.addColumn(c -> nz(c.getJ())).setHeader("J").setSortable(true);
        grid.addColumn(c -> nz(c.getG())).setHeader("G").setSortable(true);
        grid.addColumn(c -> nz(c.getP())).setHeader("P").setSortable(true);
        grid.addColumn(c -> nz(c.getA())).setHeader("A").setSortable(true);
        grid.addColumn(c -> nz(c.getSg())).setHeader("SG").setSortable(true);
        grid.addColumn(c -> nz(c.getSp())).setHeader("SP").setSortable(true);
        grid.addColumn(c -> nz(c.getDs())).setHeader("DS").setSortable(true);
        grid.addColumn(c -> nz(c.getJg())).setHeader("JG").setSortable(true);
        grid.addColumn(c -> nz(c.getJp())).setHeader("JP").setSortable(true);
        grid.addColumn(c -> nz(c.getDj())).setHeader("DJ").setSortable(true);

        grid.addColumn(c -> nz(c.getPuntosIda())).setHeader("Puntos ida");
        grid.addColumn(c -> nz(c.getJIda())).setHeader("J ida");
        grid.addColumn(c -> nz(c.getGIda())).setHeader("G ida");
        grid.addColumn(c -> nz(c.getPIda())).setHeader("P ida");
        grid.addColumn(c -> nz(c.getAIda())).setHeader("A ida");
        grid.addColumn(c -> nz(c.getSgIda())).setHeader("SG ida");
        grid.addColumn(c -> nz(c.getJgIda())).setHeader("JG ida");

        grid.addColumn(c -> nz(c.getPuntosVuelta())).setHeader("Puntos vuelta");
        grid.addColumn(c -> nz(c.getJVuelta())).setHeader("J vuelta");
        grid.addColumn(c -> nz(c.getGVuelta())).setHeader("G vuelta");
        grid.addColumn(c -> nz(c.getPVuelta())).setHeader("P vuelta");
        grid.addColumn(c -> nz(c.getAVuelta())).setHeader("A vuelta");
        grid.addColumn(c -> nz(c.getSgVuelta())).setHeader("SG vuelta");
        grid.addColumn(c -> nz(c.getJgVuelta())).setHeader("JG vuelta");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setMultiSort(true);
    }

    private void loadGrupos() {
        var grupos = new ArrayList<>(clasificacionService.listGrupos());
        if (grupos.isEmpty()) {
            grupoSelect.setItems("Grupo A");
            grupoSelect.setValue("Grupo A");
        } else {
            grupoSelect.setItems(grupos);
            if (grupoSelect.getValue() == null) grupoSelect.setValue(grupos.get(0));
        }
    }

    private void refresh() {
        String g = grupoSelect.getValue();
        if (g == null || g.isBlank()) { grid.setItems(List.of()); return; }
        grid.setItems(clasificacionService.listByGrupo(g));
    }

    private void descargarCSV() {
        String g = grupoSelect.getValue();
        if (g == null) { Notification.show("Selecciona grupo"); return; }
        List<ClasificacionEntry> rows = clasificacionService.listByGrupo(g);
        String csv = toCsv(rows);
        String base64 = java.util.Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8));
        getUI().get().getPage().open("data:text/csv;base64," + base64);
    }

    // ----------------- CSV helpers -----------------
    private List<ClasificacionEntry> parseClasificacionCsv(String content) {
        if (content == null) content = "";
        String[] lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        if (lines.length == 0) throw new IllegalArgumentException("CSV vacío");
        String header = lines[0].trim();

        String sep = detectSep(header);
        int start = header.toLowerCase().contains("equipos") ? 1 : 0;

        List<ClasificacionEntry> out = new ArrayList<>();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) continue;
            String[] c = line.split(java.util.regex.Pattern.quote(sep), -1);
            if (c.length < 27) {
                c = Arrays.copyOf(c, 27);
                for (int k = 0; k < c.length; k++) if (c[k] == null) c[k] = "";
            }

            ClasificacionEntry row = new ClasificacionEntry();
            row.setEquipo(c[0].trim());

            row.setPuntos(ni(c[2]));  row.setJ(ni(c[3]));  row.setG(ni(c[4]));  row.setP(ni(c[5]));
            row.setA(ni(c[6]));       row.setSg(ni(c[7])); row.setSp(ni(c[8])); row.setDs(ni(c[9]));
            row.setJg(ni(c[10]));     row.setJp(ni(c[11]));row.setDj(ni(c[12]));

            row.setPuntosIda(ni(c[13])); row.setJIda(ni(c[14])); row.setGIda(ni(c[15])); row.setPIda(ni(c[16]));
            row.setAIda(ni(c[17])); row.setSgIda(ni(c[18])); row.setJgIda(ni(c[19]));

            row.setPuntosVuelta(ni(c[20])); row.setJVuelta(ni(c[21])); row.setGVuelta(ni(c[22]));
            row.setPVuelta(ni(c[23])); row.setAVuelta(ni(c[24])); row.setSgVuelta(ni(c[25])); row.setJgVuelta(ni(c[26]));

            out.add(row);
        }
        return out;
    }

    private String detectSep(String header) {
        if (header.contains("\t")) return "\t";
        if (header.contains(";")) return ";";
        return ","; // por defecto coma
    }

    private Integer ni(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.isEmpty()) return 0;
        try { return Integer.valueOf(t.replace(".", "").replace(",", "")); }
        catch (Exception e) { return 0; }
    }

    private String toCsv(List<ClasificacionEntry> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Equipos,Puntos,J,G,P,A,SG,SP,DS,JG,JP,DJ,Puntos ida,J ida,G ida,P ida,A ida,SG ida,JG ida,Puntos vuelta,J vuelta,G vuelta,P vuelta,A vuelta,SG vuelta,JG vuelta\n");
        for (var r : rows) {
            sb.append(escape(r.getEquipo())).append(",");
            sb.append(nz(r.getPuntos())).append(",")
                    .append(nz(r.getJ())).append(",").append(nz(r.getG())).append(",").append(nz(r.getP())).append(",").append(nz(r.getA())).append(",")
                    .append(nz(r.getSg())).append(",").append(nz(r.getSp())).append(",").append(nz(r.getDs())).append(",")
                    .append(nz(r.getJg())).append(",").append(nz(r.getJp())).append(",").append(nz(r.getDj())).append(",")
                    .append(nz(r.getPuntosIda())).append(",").append(nz(r.getJIda())).append(",").append(nz(r.getGIda())).append(",").append(nz(r.getPIda())).append(",").append(nz(r.getAIda())).append(",").append(nz(r.getSgIda())).append(",").append(nz(r.getJgIda())).append(",")
                    .append(nz(r.getPuntosVuelta())).append(",").append(nz(r.getJVuelta())).append(",").append(nz(r.getGVuelta())).append(",").append(nz(r.getPVuelta())).append(",").append(nz(r.getAVuelta())).append(",").append(nz(r.getSgVuelta())).append(",").append(nz(r.getJgVuelta()))
                    .append("\n");
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        String t = s.replace("\"","\"\"");
        if (t.contains(",") || t.contains("\"")) return "\""+t+"\"";
        return t;
    }

    private int nz(Integer x) { return x == null ? 0 : x; }
}
