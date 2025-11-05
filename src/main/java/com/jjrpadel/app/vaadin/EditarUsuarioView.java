package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Equipo;
import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "usuarios/editar", layout = MainLayout.class)
@PageTitle("Editar usuario")
@PermitAll // 👈 deja pasar y validamos dentro si es ADMIN o dueño
public class EditarUsuarioView extends VerticalLayout implements HasUrlParameter<String> {

    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private Usuario usuarioActual;

    private final TextField nombre = new TextField("Nombre");
    private final TextField apellidos = new TextField("Apellidos");
    private final TextField username = new TextField("Username");
    private final Select<Role> rol = new Select<>();
    private final IntegerField puntos = new IntegerField("Puntos");
    private final Select<Equipo> equipo = new Select<>();

    private final Button guardar = new Button("Guardar");
    private final Button volver = new Button("Volver");
    private final Button cambiarPassword = new Button("Cambiar contraseña");

    private final Binder<Usuario> binder = new Binder<>(Usuario.class);

    public EditarUsuarioView(UsuarioService usuarioService, AuthenticationContext auth) {
        this.usuarioService = usuarioService;
        this.auth = auth;

        setPadding(true);
        setSpacing(true);
        add(new H2("Mi Perfil"));

        rol.setItems(Role.values());
        rol.setLabel("Rol");

        equipo.setItems(Equipo.values());
        equipo.setLabel("Equipo");

        puntos.setMin(0);
        username.setReadOnly(true); // no editable

        FormLayout form = new FormLayout(nombre, apellidos, username, rol, equipo, puntos);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        binder.bindInstanceFields(this);

        guardar.addClickListener(e -> guardarCambios());
        cambiarPassword.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("usuarios/cambiar-password/" + usuarioActual.getId()))
        );
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("usuarios")));

        add(form, new HorizontalLayout(guardar, cambiarPassword, volver));
    }

    @Override
    public void setParameter(BeforeEvent event, String id) {
        String principal = auth.getPrincipalName().orElse(null);
        if (principal == null) {
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }

        // Si el parámetro es "self", cargar el propio usuario logueado
        if ("self".equalsIgnoreCase(id)) {
            usuarioActual = usuarioService.findByUsername(principal).orElse(null);
        } else {
            usuarioActual = usuarioService.findById(id).orElse(null);
        }

        if (usuarioActual == null) {
            Notification.show("Usuario no encontrado");
            getUI().ifPresent(ui -> ui.navigate("usuarios"));
            return;
        }

        boolean esAdmin = isUserInRole("ADMIN");
        boolean esDueno = principal.equalsIgnoreCase(usuarioActual.getUsername());

        if (!esAdmin && !esDueno) {
            Notification.show("Acceso denegado");
            getUI().ifPresent(ui -> ui.navigate("usuarios"));
            return;
        }

        // Limitar edición si es su perfil personal
        if (!esAdmin) {
            rol.setReadOnly(true);
            username.setReadOnly(true);
            // Si quieres también bloquear el equipo:
            // equipo.setReadOnly(true);
            puntos.setReadOnly(true);
        }

        binder.readBean(usuarioActual);
    }


    private void guardarCambios() {
        try {
            binder.writeBean(usuarioActual);
            usuarioService.save(usuarioActual);
            Notification.show("Cambios guardados correctamente", 3000, Notification.Position.TOP_CENTER);

            // Admin vuelve al listado, usuario normal a su perfil
            if (isUserInRole("ADMIN")) {
                getUI().ifPresent(ui -> ui.navigate("usuarios"));
            } else {
                getUI().ifPresent(ui -> ui.navigate("perfil"));
            }
        } catch (ValidationException e) {
            Notification.show("Revisa los campos del formulario", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private boolean isUserInRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (("ROLE_" + role).equals(a.getAuthority())) return true;
        }
        return false;
    }
}