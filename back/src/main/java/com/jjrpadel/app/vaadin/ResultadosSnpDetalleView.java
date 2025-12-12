package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.*;
import com.jjrpadel.app.service.PartidoService;
import com.jjrpadel.app.service.UsuarioService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Route(value = "resultados-snp", layout = MainLayout.class)
@PageTitle("Detalle resultado SNP")
@PermitAll
@RequiredArgsConstructor
public class ResultadosSnpDetalleView extends VerticalLayout implements HasUrlParameter<String> {

    private final PartidoService partidoService;
    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private Partido partido;

    // Controles
    private final RadioButtonGroup<String> localVisitante = new RadioButtonGroup<>();
    private final List<ParResultadoUI> filas = new ArrayList<>();
    private final Button guardar = new Button("Guardar");
    private final Button volver = new Button("Volver");

    private boolean puedeEditar = false;

    @Override
    public void setParameter(BeforeEvent event, String partidoId) {
        setSizeFull();
        setPadding(true);
        setMaxWidth("1000px");

        partido = partidoService.findById(partidoId).orElse(null);
        if (partido == null || !partido.isEsSnp()) {
            Notification.show("Partido SNP no encontrado");
            getUI().ifPresent(ui -> ui.navigate("resultados-snp"));
            return;
        }

        var current = auth.getPrincipalName().flatMap(usuarioService::findByUsername).orElse(null);
        if (current == null) {
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }
        // Permisos: ADMIN o CAPITAN del mismo equipo puede editar; USER solo ver
        puedeEditar = current.getRol() == Role.ADMIN
                   || (current.getRol() == Role.CAPITAN && current.getEquipo() == partido.getEquipo());

        // Asegura estructura básica desde convocatoria
        try {
            partido = partidoService.initSnpResultadoDesdeConvocatoria(partido);
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
            getUI().ifPresent(ui -> ui.navigate("partidos")); // no hay convocatoria
            return;
        }

        buildUI();
    }

    private void buildUI() {
        removeAll();

        add(new H2("Resultado SNP"));
        add(new Paragraph(
                (partido.getFecha() == null ? "" : partido.getFecha().toString()) + " " +
                (partido.getHora() == null ? "" : partido.getHora().toString()) +
                " · " + partido.getLocalizacion() + " · Equipo " + partido.getEquipo()
        ));

        // Local / Visitante
        localVisitante.setLabel("Nuestro equipo jugó como");
        localVisitante.setItems("Local", "Visitante");
        localVisitante.setValue(Boolean.TRUE.equals(partido.getSnpResultado().getSomosLocales()) ? "Local" : "Visitante");
        localVisitante.setReadOnly(!puedeEditar);
        add(localVisitante);

        // 5 filas (una por pareja de la convocatoria)
        var cont = new VerticalLayout();
        cont.setPadding(false);
        cont.setSpacing(false);

        filas.clear();
        int idx = 1;
        for (var r : partido.getSnpResultado().getResultados()) {
            var comp = new ParResultadoUI(idx++, r, puedeEditar);
            filas.add(comp);
            cont.add(comp.wrapper);
        }
        add(cont);

        // Botones
        guardar.setEnabled(puedeEditar);
        guardar.addClickListener(e -> guardarCambios());
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("resultados-snp")));

        add(new HorizontalLayout(guardar, volver));
    }

    private void guardarCambios() {
        // Vuelca UI -> modelo
        var snp = partido.getSnpResultado();
        snp.setSomosLocales("Local".equals(localVisitante.getValue()));
        for (int i = 0; i < filas.size(); i++) {
            var f = filas.get(i);
            var r = snp.getResultados().get(i);

            r.setNuestros1Nombre(f.nuestros1.getValue());
            r.setNuestros2Nombre(f.nuestros2.getValue());
            r.setPuntosJ1(f.pJ1.getValue() == null ? 0 : f.pJ1.getValue());
            r.setPuntosJ2(f.pJ2.getValue() == null ? 0 : f.pJ2.getValue());

            // (ya tenías) rivales
            r.setRival1Nombre(f.rival1.getValue());
            r.setRival2Nombre(f.rival2.getValue());
            r.setPuntosRival1(f.pRival1.getValue() == null ? 0 : f.pRival1.getValue());
            r.setPuntosRival2(f.pRival2.getValue() == null ? 0 : f.pRival2.getValue());

            // (ya tenías) sets
            r.setJugadoTercerSet(Boolean.TRUE.equals(f.chk3.getValue()));
            r.setS1N(nz(f.s1n.getValue())); r.setS1R(nz(f.s1r.getValue()));
            r.setS2N(nz(f.s2n.getValue())); r.setS2R(nz(f.s2r.getValue()));
            if (r.getJugadoTercerSet()) { r.setS3N(nz(f.s3n.getValue())); r.setS3R(nz(f.s3r.getValue())); }
            else { r.setS3N(null); r.setS3R(null); }
        }
        partidoService.guardarSnpResultado(partido, snp);
        Notification.show("Resultado guardado");
        getUI().ifPresent(ui -> ui.navigate("resultados-snp"));
    }

    /**
     * Componente de una fila (pareja) con nuestras info y campos rivales.
     */
    private class ParResultadoUI {
        final Div wrapper = new Div();

        final Span nuestrosLbl = new Span();
        final Span nuestrosPts = new Span();

        final TextField rival1 = new TextField("Rival 1");
        final IntegerField pRival1 = new IntegerField("Pts R1");
        final TextField rival2 = new TextField("Rival 2");
        final IntegerField pRival2 = new IntegerField("Pts R2");

        final IntegerField s1n = new IntegerField("Set 1 (nos)");
        final IntegerField s1r = new IntegerField("Set 1 (riv)");
        final IntegerField s2n = new IntegerField("Set 2 (nos)");
        final IntegerField s2r = new IntegerField("Set 2 (riv)");
        final IntegerField s3n = new IntegerField("Set 3 (nos)");
        final IntegerField s3r = new IntegerField("Set 3 (riv)");
        final com.vaadin.flow.component.checkbox.Checkbox chk3 = new com.vaadin.flow.component.checkbox.Checkbox("Se jugó 3er set");

        final TextField   nuestros1 = new TextField("Nuestro jugador 1");
        final IntegerField pJ1      = new IntegerField("Pts J1");
        final TextField   nuestros2 = new TextField("Nuestro jugador 2");
        final IntegerField pJ2      = new IntegerField("Pts J2");


        ParResultadoUI(int numero, ResultadoPareja r, boolean editable) {
            wrapper.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            wrapper.getStyle().set("border-radius", "12px");
            wrapper.getStyle().set("padding", "12px");
            wrapper.getStyle().set("margin-bottom", "12px");

            if (r.getNuestros1Nombre() == null || r.getNuestros1Nombre().isBlank()) {
                var u1o = usuarioService.findByUsername(r.getJugador1());
                r.setNuestros1Nombre(u1o.map(u -> (ns(u.getNombre()) + " " + ns(u.getApellidos())).trim())
                        .orElse(r.getJugador1()));
            }
            if (r.getNuestros2Nombre() == null || r.getNuestros2Nombre().isBlank()) {
                var u2o = usuarioService.findByUsername(r.getJugador2());
                r.setNuestros2Nombre(u2o.map(u -> (ns(u.getNombre()) + " " + ns(u.getApellidos())).trim())
                        .orElse(r.getJugador2()));
            }

            nuestros1.setValue(r.getNuestros1Nombre());
            nuestros2.setValue(r.getNuestros2Nombre());
            pJ1.setMin(0); pJ2.setMin(0);
            pJ1.setValue(r.getPuntosJ1() == null ? 0 : r.getPuntosJ1());
            pJ2.setValue(r.getPuntosJ2() == null ? 0 : r.getPuntosJ2());

            nuestros1.setReadOnly(!editable);
            nuestros2.setReadOnly(!editable);
            pJ1.setReadOnly(!editable);
            pJ2.setReadOnly(!editable);

            // --- bloque rivales que ya tenías ---
            // rival1, pRival1, rival2, pRival2 ...

            // Form de “nuestros”
            FormLayout formNuestros = new FormLayout(nuestros1, pJ1, nuestros2, pJ2);
            formNuestros.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("700px", 2)
            );


            var u1 = usuarioService.findByUsername(r.getJugador1()).orElse(null);
            var u2 = usuarioService.findByUsername(r.getJugador2()).orElse(null);

            String n1 = (u1 == null) ? r.getJugador1() :
                    ((ns(u1.getNombre()) + " " + ns(u1.getApellidos())).trim() + " [" + u1.getUsername() + "]");
            String n2 = (u2 == null) ? r.getJugador2() :
                    ((ns(u2.getNombre()) + " " + ns(u2.getApellidos())).trim() + " [" + u2.getUsername() + "]");

            int pj1 = r.getPuntosJ1() == null ? 0 : r.getPuntosJ1();
            int pj2 = r.getPuntosJ2() == null ? 0 : r.getPuntosJ2();

            nuestrosLbl.setText(numero + ") " + n1 + " + " + n2);
            nuestrosPts.setText("Nuestros puntos: " + pj1 + " + " + pj2 + " = " + (pj1 + pj2));

            rival1.setValue(r.getRival1Nombre() == null ? "" : r.getRival1Nombre());
            rival2.setValue(r.getRival2Nombre() == null ? "" : r.getRival2Nombre());
            pRival1.setMin(0); pRival2.setMin(0);
            pRival1.setValue(r.getPuntosRival1() == null ? 0 : r.getPuntosRival1());
            pRival2.setValue(r.getPuntosRival2() == null ? 0 : r.getPuntosRival2());

            rival1.setReadOnly(!editable);
            rival2.setReadOnly(!editable);
            pRival1.setReadOnly(!editable);
            pRival2.setReadOnly(!editable);

            var top = new HorizontalLayout(new H4("Enfrentamiento Parejas " + String.valueOf(numero)));
            top.setAlignItems(Alignment.CENTER);
            top.setSpacing(true);

            var form = new FormLayout(rival1, pRival1, rival2, pRival2);
            form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
            );

            //wrapper.add(top, form);

            for (var f : new IntegerField[]{s1n,s1r,s2n,s2r,s3n,s3r}) { f.setMin(0); f.setMax(99); }

            s1n.setValue(r.getS1N() == null ? 0 : r.getS1N());
            s1r.setValue(r.getS1R() == null ? 0 : r.getS1R());
            s2n.setValue(r.getS2N() == null ? 0 : r.getS2N());
            s2r.setValue(r.getS2R() == null ? 0 : r.getS2R());
            chk3.setValue(Boolean.TRUE.equals(r.getJugadoTercerSet()));
            s3n.setValue(r.getS3N() == null ? 0 : r.getS3N());
            s3r.setValue(r.getS3R() == null ? 0 : r.getS3R());

            // Habilitación según el checkbox
            s3n.setEnabled(chk3.getValue());
            s3r.setEnabled(chk3.getValue());
            chk3.addValueChangeListener(ev -> {
                boolean en = Boolean.TRUE.equals(ev.getValue());
                s3n.setEnabled(en);
                s3r.setEnabled(en);
                if (!en) { s3n.setValue(0); s3r.setValue(0); }
            });

            s1n.setReadOnly(!editable); s1r.setReadOnly(!editable);
            s2n.setReadOnly(!editable); s2r.setReadOnly(!editable);
            s3n.setReadOnly(!editable); s3r.setReadOnly(!editable);
            chk3.setReadOnly(!editable);

            // Form de sets
            FormLayout setsForm = new FormLayout(
                    s1n, s1r,
                    s2n, s2r,
                    chk3, new Span(""), // para cuadrar 2 columnas
                    s3n, s3r
            );
            setsForm.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("700px", 2)
            );

            wrapper.add(top,
                    new H5("Nuestros jugadores"),
                    formNuestros,
                    new H5("Rivales"),
                    form,
                    new H5("Resultado por sets"),
                    setsForm);

            String marcador =
                    (r.getS1N()==null?0:r.getS1N()) + "-" + (r.getS1R()==null?0:r.getS1R()) + ", " +
                            (r.getS2N()==null?0:r.getS2N()) + "-" + (r.getS2R()==null?0:r.getS2R()) +
                            (Boolean.TRUE.equals(r.getJugadoTercerSet()) ?
                                    ", " + (r.getS3N()==null?0:r.getS3N()) + "-" + (r.getS3R()==null?0:r.getS3R()) : "");

            wrapper.add(new Span("Marcador: " + marcador));
        }
    }

    private static String ns(String s) { return s == null ? "" : s; }
    private static int nz(Integer i) { return i == null ? 0 : i; }
}
