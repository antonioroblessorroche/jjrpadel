# JJRPadel — Vaadin CRUD Usuario (MongoDB + Lombok)

CRUD de usuarios con Vaadin Flow 24, Spring Boot 3 y MongoDB. Seguridad con Spring Security (+BCrypt).

## Entidad Usuario
- nombre, apellidos
- rol (**ADMIN**, **USER**)
- puntos (Integer)
- **equipo** (String) — equipo al que pertenece
- username (único), password (hash BCrypt)

## Requisitos
- Java 17
- Maven 3.8+
- Un clúster de MongoDB (Atlas o local)

## Configuración MongoDB Atlas
Exporta tu URI como variable de entorno **MONGODB_URI**. Ejemplo con tu clúster:

```bash
export MONGODB_URI="mongodb+srv://<db_username>:<db_password>@jjrpadel.qc3qicu.mongodb.net/vaadin_demo?retryWrites=true&w=majority&appName=jjrpadel"
```

> Sustituye `<db_username>` y `<db_password>` por tus credenciales.

## Ejecutar
```bash
mvn spring-boot:run
```
Accede a `http://localhost:8080/` → te redirigirá a login. Credenciales iniciales:
- **admin / admin123** (rol ADMIN)

## Rutas
- `/usuarios` — CRUD (solo ADMIN)

## Notas
- La contraseña se **hash**ea con BCrypt al crear/editar.
- Si editas un usuario y dejas el campo contraseña vacío, se conserva el hash existente.
- El campo `equipo` es libre (p.ej. "Equipo A", "Equipo B", etc.).
