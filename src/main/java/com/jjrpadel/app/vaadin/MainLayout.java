package com.jjrpadel.app.vaadin;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class MainLayout extends AppLayout {

    private final AuthenticationContext auth;

    public MainLayout(AuthenticationContext auth) {
        this.auth = auth;

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("JJR Padel");
        title.getStyle().set("font-size", "1.25rem");
        title.getStyle().set("margin", "0");

        String username = auth.getPrincipalName().orElse("Invitado");
        Avatar avatar = new Avatar(username);
        avatar.setThemeName("large");

        Span userName = new Span(username);
        userName.getStyle().set("margin-inline-start", "0.5rem");

        // ✅ Botón de salir funcional con AuthenticationContext
        Button logoutBtn = new Button("Salir", e -> auth.logout());
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout right = new HorizontalLayout(avatar, userName, logoutBtn);
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(toggle, title, right);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Mi perfil", "usuarios/editar/self"));

        // Solo ADMIN: Usuarios y Alta usuario
        if (isUserInRole("ADMIN")) {
            nav.addItem(new SideNavItem("Usuarios", UsuarioCrudView.class));
            nav.addItem(new SideNavItem("Alta usuario", AdminAltaUsuarioView.class));
        }

        addToDrawer(nav);
    }
    private boolean isUserInRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (("ROLE_" + role).equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}