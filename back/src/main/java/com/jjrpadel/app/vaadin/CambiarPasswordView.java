package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Usuario;
import com.jjrpadel.app.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
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

@Route(value = "usuarios/cambiar-password", layout = MainLayout.class)
@PageTitle("Cambiar contraseña")
@PermitAll
public class CambiarPasswordView extends VerticalLayout implements HasUrlParameter<String> {

    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;
    private Usuario usuarioActual;

    private final PasswordField passwordNueva = new PasswordField("Nueva contraseña");
    private final PasswordField passwordConfirmacion = new PasswordField("Confirmar contraseña");
    private final Button guardar = new Button("Guardar");
    private final Button cancelar = new Button("Cancelar");

    private final Binder<Usuario> binder = new Binder<>(Usuario.class);

    public CambiarPasswordView(UsuarioService usuarioService, AuthenticationContext auth) {
        this.usuarioService = usuarioService;
        this.auth = auth;

        setPadding(true);
        setSpacing(true);
        add(new H2("Cambiar contraseña"));

        passwordNueva.setRevealButtonVisible(true);
        passwordConfirmacion.setRevealButtonVisible(true);

        FormLayout form = new FormLayout(passwordNueva, passwordConfirmacion);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        guardar.addClickListener(e -> cambiarPassword());
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("usuarios")));

        add(form, new HorizontalLayout(guardar, cancelar));
    }

    @Override
    public void setParameter(BeforeEvent event, String id) {
        String principal = auth.getPrincipalName().orElse(null);
        if (principal == null) {
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }

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
        }
    }

    private void cambiarPassword() {
        String pass1 = passwordNueva.getValue();
        String pass2 = passwordConfirmacion.getValue();

        if (pass1 == null || pass1.isBlank() || pass2 == null || pass2.isBlank()) {
            Notification.show("Debe rellenar ambos campos");
            return;
        }

        if (!pass1.equals(pass2)) {
            Notification.show("Las contraseñas no coinciden");
            return;
        }

        try {
            usuarioActual.setPassword(pass1);
            usuarioService.save(usuarioActual);
            Notification.show("Contraseña actualizada correctamente", 3000, Notification.Position.TOP_CENTER);
            getUI().ifPresent(ui -> ui.navigate("usuarios/editar/" + usuarioActual.getId()));
        } catch (Exception e) {
            Notification.show("Error al cambiar la contraseña", 3000, Notification.Position.TOP_CENTER);
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
