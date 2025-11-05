package com.jjrpadel.app.vaadin;

import com.jjrpadel.app.model.Role;
import com.jjrpadel.app.model.Usuario;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import jakarta.annotation.security.RolesAllowed;

public class UsuarioForm extends FormLayout {

    TextField nombre = new TextField("Nombre");
    TextField apellidos = new TextField("Apellidos");
    TextField username = new TextField("Username");
    PasswordField password = new PasswordField("Password (solo para cambiar)");
    IntegerField puntos = new IntegerField("Puntos");
    Select<Role> rol = new Select<>();
    TextField equipo = new TextField("Equipo");

    Button save = new Button("Guardar");
    Button cancel = new Button("Cancelar");

    Binder<Usuario> binder = new Binder<>(Usuario.class);

    public UsuarioForm() {
        rol.setItems(Role.values());
        rol.setLabel("Rol");
        puntos.setMin(0);
        add(nombre, apellidos, username, password, rol, puntos, equipo, save, cancel);
        binder.bindInstanceFields(this);
    }
}
