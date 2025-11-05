package com.jjrpadel.app.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nombre;
    private String apellidos;
    private Role rol;
    private Integer puntos;
    private Equipo equipo;
    @Indexed(unique = true)
    private String username;

    /** Hash BCrypt */
    private String password;
}
