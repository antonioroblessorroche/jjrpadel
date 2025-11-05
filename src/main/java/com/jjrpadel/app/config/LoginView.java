package com.jjrpadel.app.config;

import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("login")
@PageTitle("Login")
public class LoginView extends LoginOverlay {

    public LoginView() {
        setTitle("JJR Padel");
        setDescription("Acceso");
        setOpened(true);
        setAction("login");
        setForgotPasswordButtonVisible(false);

    }
}