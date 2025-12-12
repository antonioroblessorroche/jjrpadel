package com.jjrpadel.app.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route("")               // ruta raíz
@PermitAll
public class RootRedirectView extends Div implements BeforeEnterObserver {
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo("usuarios/editar/self");
    }
}