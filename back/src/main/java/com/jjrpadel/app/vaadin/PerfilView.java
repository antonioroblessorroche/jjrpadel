package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Equipo;
import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.prepost.PreAuthorize;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Vista "Mi Perfil"
 * Permite al usuario autenticado ver y editar sus propios datos.
 */
@Route(value = "perfil", layout = MainLayout.class)
@PageTitle("Mi Perfil")
@PreAuthorize("isAuthenticated()")
@PermitAll
public class PerfilView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private TextField nombre = new TextField("Nombre");
    private TextField apellidos = new TextField("Apellidos");
    private TextField username = new TextField("Username");
    private TextField rol = new TextField("Rol");
    private IntegerField puntos = new IntegerField("Puntos");
    private TextField equipo = new TextField("Equipo");

    private Button guardar = new Button("Guardar cambios");

    private Usuario usuarioActual;

    public PerfilView(UsuarioService usuarioService, AuthenticationContext auth) {
        this.usuarioService = usuarioService;
        this.auth = auth;

        setSizeFull();
        setPadding(true);
        add(new H2("Mi perfil"));

        username.setReadOnly(true);
        rol.setReadOnly(true);
        puntos.setMin(0);

        FormLayout form = new FormLayout(nombre, apellidos, username, rol, puntos, equipo);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        add(form, guardar);

        cargarDatos();

        guardar.addClickListener(e -> {
            if (usuarioActual != null) {
                usuarioActual.setNombre(nombre.getValue());
                usuarioActual.setApellidos(apellidos.getValue());
                usuarioActual.setPuntos(puntos.getValue());
                usuarioActual.setEquipo(Equipo.valueOf(equipo.getValue()));
                usuarioService.save(usuarioActual);
                Notification.show("Perfil actualizado");
            }
        });
    }

    private void cargarDatos() {
        String user = auth.getPrincipalName().orElse(null);
        if (user != null) {
            usuarioActual = usuarioService.findByUsername(user).orElse(null);
            if (usuarioActual != null) {
                nombre.setValue(usuarioActual.getNombre() != null ? usuarioActual.getNombre() : "");
                apellidos.setValue(usuarioActual.getApellidos() != null ? usuarioActual.getApellidos() : "");
                username.setValue(usuarioActual.getUsername());
                rol.setValue(usuarioActual.getRol() != null ? usuarioActual.getRol().name() : "");
                puntos.setValue(usuarioActual.getPuntos() != null ? usuarioActual.getPuntos() : 0);
                equipo.setValue(usuarioActual.getEquipo() != null ? usuarioActual.getEquipo().name(): "");
            }
        }
    }
}