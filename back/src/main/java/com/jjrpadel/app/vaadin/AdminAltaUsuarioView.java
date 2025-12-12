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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/alta-usuario", layout = MainLayout.class)
@PageTitle("Alta de usuario")
@RolesAllowed({"ADMIN", "CAPITAN"})
public class AdminAltaUsuarioView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final AuthenticationContext auth;

    private final TextField nombre = new TextField("Nombre");
    private final TextField apellidos = new TextField("Apellidos");
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final Select<Role> rol = new Select<>();
    private final IntegerField puntos = new IntegerField("Puntos");
    private final Select<Equipo> equipoSelect = new Select<>();

    private final Button guardar = new Button("Crear usuario");
    private final Button limpiar = new Button("Limpiar");

    private final Binder<Usuario> binder = new BeanValidationBinder<>(Usuario.class);

    public AdminAltaUsuarioView(UsuarioService usuarioService, AuthenticationContext auth) {
        this.usuarioService = usuarioService;
        this.auth = auth;

        setPadding(true);
        setSpacing(true);
        add(new H2("Alta de usuario"));

        // Campos
        rol.setLabel("Rol");
        rol.setItems(Role.values());
        rol.setRequiredIndicatorVisible(true);

        puntos.setMin(0);
        puntos.setValue(0);

        equipoSelect.setLabel("Equipo");
        equipoSelect.setItems(Equipo.values());
        equipoSelect.setItemLabelGenerator(Enum::name);
        equipoSelect.setRequiredIndicatorVisible(true);

        password.setRevealButtonVisible(true);
        nombre.setRequiredIndicatorVisible(true);
        username.setRequiredIndicatorVisible(true);
        password.setRequiredIndicatorVisible(true);

        // --- Binding explícito ---
        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Usuario::getNombre, Usuario::setNombre);

        binder.forField(apellidos)
                .bind(Usuario::getApellidos, Usuario::setApellidos);

        binder.forField(username)
                .asRequired("El username es obligatorio")
                .withValidator(new StringLengthValidator("Entre 3 y 50 caracteres", 3, 50))
                .withValidator(u -> usuarioService.findByUsername(u).isEmpty(), "El username ya existe")
                .bind(Usuario::getUsername, Usuario::setUsername);

        binder.forField(password)
                .asRequired("La contraseña es obligatoria")
                .withValidator(pw -> pw != null && pw.length() >= 6, "Mínimo 6 caracteres")
                .bind(Usuario::getPassword, Usuario::setPassword);

        binder.forField(rol)
                .asRequired("Selecciona un rol")
                .bind(Usuario::getRol, Usuario::setRol);

        binder.forField(puntos)
                .bind(Usuario::getPuntos, Usuario::setPuntos);

        binder.forField(equipoSelect)
                .asRequired("Selecciona un equipo")
                .bind(Usuario::getEquipo, Usuario::setEquipo);

        // Layout
        FormLayout form = new FormLayout(
                nombre, apellidos, username, password,
                rol, puntos, equipoSelect
        );
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        // Reglas según quién crea: ADMIN vs CAPITAN
        aplicarReglasDeCapitanSiCorresponde();

        guardar.addClickListener(e -> crearUsuario());
        limpiar.addClickListener(e -> binder.readBean(new Usuario()));

        add(form, new HorizontalLayout(guardar, limpiar));
    }

    private void aplicarReglasDeCapitanSiCorresponde() {
        var principal = auth.getPrincipalName().orElse(null);
        if (principal == null) return;

        var current = usuarioService.findByUsername(principal).orElse(null);
        if (current == null) return;

        if (current.getRol() == Role.CAPITAN) {
            // El capitán solo crea USER y en su propio equipo
            rol.setValue(Role.USER);
            rol.setReadOnly(true);

            equipoSelect.setValue(current.getEquipo());
            equipoSelect.setReadOnly(true);
        }
        // Si es ADMIN, no tocamos nada (elige libremente)
    }

    private void crearUsuario() {
        Usuario nuevo = new Usuario();
        try {
            binder.writeBean(nuevo);  // password en claro → UsuarioService la hashea
            usuarioService.save(nuevo);
            Notification.show("Usuario creado correctamente", 3000, Notification.Position.TOP_CENTER);
            binder.readBean(new Usuario()); // limpiar
        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 4000, Notification.Position.TOP_CENTER);
        } catch (Exception ex) {
            Notification.show("Error al crear: " + (ex.getMessage() == null ? "" : ex.getMessage()),
                    4000, Notification.Position.TOP_CENTER);
        }
    }
}
