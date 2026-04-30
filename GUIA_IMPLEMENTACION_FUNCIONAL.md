# Guía didáctica de implementación: Login, sesión y seguridad de contraseña

> Esta guía está pensada como un tutorial paso a paso, escrito en tono de profesor.
> Vamos a construir, **dentro de tu proyecto actual**, toda la parte de:
>
> 1. Modelo de usuario y persistencia.
> 2. Registro con contraseña hasheada.
> 3. Login que devuelve un JWT.
> 4. Recuperación de contraseña (token temporal + reseteo).
> 5. **Cancelación de cuenta** (punto 4 del enunciado).
> 6. Validación del JWT desde `esientradas-master` (`ExternalController`).
> 7. Configuración de seguridad (Spring Security) con las rutas correctas.
> 8. CORS para que el frontend Angular pueda llamar a los dos backends.
>
> Respetamos la **estructura ya existente**:
>
> ```
> esiusuarios/src/main/java/edu/esi/ds/esiusuarios/
>   ├── dao/         → repositorios JPA
>   ├── dto/         → objetos de entrada/salida HTTP
>   ├── http/        → controladores REST
>   │     ├── UserController         (controlador para Web / front-end)
>   │     └── ExternalController     (controlador para Sistemas, usado por esientradas)
>   ├── model/       → entidades JPA
>   ├── security/    → configuración de Spring Security + filtros JWT
>   └── services/    → lógica de negocio
> ```
>
> Tus diagramas dibujados a mano distinguen "controladores para Web" de "controladores para Sistemas".
> En tu proyecto ambos están en el mismo paquete `http/`. Si quieres respetar el diagrama de forma
> estricta puedes crear dos sub-paquetes (`http/web/` y `http/sistemas/`) y mover ahí cada controlador.
> Funcionalmente es **idéntico**: solo es organización.
>
> Cada paso te dice **qué archivo abrir**, **por qué hacemos lo que hacemos** y **el código completo** que tiene que quedar.

---

## Paso 0. Dependencias necesarias (`esiusuarios/pom.xml`)

Antes de tocar código, comprueba que tu `pom.xml` tiene estas dependencias (en tu proyecto **ya están**):

- `spring-boot-starter-web` → controladores REST.
- `spring-boot-starter-security` → BCrypt + filtros de seguridad.
- `spring-boot-starter-data-jpa` → `JpaRepository`.
- `mssql-jdbc` → driver SQL Server (BD usuarios).
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (versión 0.11.5) → generar y validar JWT.
- `spring-boot-starter-mail` → **obligatoria**, para enviar el email de recuperación al correo del usuario.

Añade en tu `esiusuarios/pom.xml` (dentro de `<dependencies>`) si no está ya:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

> El email de recuperación se envía **al correo del propio usuario que lo solicita** (es el requisito real de “olvidé mi contraseña”). En el `EmailService` mantenemos un *fallback* a consola si el SMTP no está configurado, para que la defensa no se rompa si falla la red.

---

## Paso 1. Configuración (`application.properties`)

Abre `esiusuarios/src/main/resources/application.properties` y déjalo así:

```properties
spring.application.name=esiusuarios
server.port=8081

# --- Base de datos SQL Server ---
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=DBUsuarios;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=TU_PASSWORD_SQL
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect

# --- JWT ---
# Clave HMAC para firmar tokens. EN PRODUCCION ponla en variable de entorno.
app.jwt.secret=cambia-esto-por-una-clave-larga-y-aleatoria-de-al-menos-32-bytes
app.jwt.expiration-ms=86400000

# --- Recuperacion de contrasena ---
app.reset.expiration-minutes=20

# --- SMTP para enviar el email de recuperacion al correo del usuario ---
# Ejemplo con Gmail. Para Gmail necesitas una "App Password" (no tu password normal):
#   https://myaccount.google.com/apppasswords
# Si usas Outlook/Office365 -> host=smtp.office365.com, port=587
# Si usas un servidor de pruebas tipo Mailtrap -> rellena con sus credenciales.
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu_correo@gmail.com
spring.mail.password=TU_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Direccion que aparecera como remitente en el email
app.mail.from=tu_correo@gmail.com
```

> **Importante:** si dejas `spring.mail.username` o `app.mail.from` vacíos, el `EmailService`
> detecta que SMTP no está configurado y cae automáticamente al modo consola (no rompe la app).

**Por qué:**
- Externalizamos el `secret` del JWT (antes estaba *hardcoded* en `JwtService`, eso es un fallo OWASP A02).
- Externalizamos también la expiración del JWT y del token de reset.

---

## Paso 2. Entidad `User` limpia (`model/User.java`)

La entidad actual tiene campos heredados del demo en memoria (`name`, `password` en claro, `token`). La dejamos **solo con campos persistentes reales**.

Reemplaza el contenido de [esiusuarios/src/main/java/edu/esi/ds/esiusuarios/model/User.java](esiusuarios/src/main/java/edu/esi/ds/esiusuarios/model/User.java):

```java
package edu.esi.ds.esiusuarios.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    public User() {}

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

**Por qué:**
- Quitamos `password` (en claro) y `token` (un usuario puede tener N tokens activos, no es atributo del usuario).
- `email` con índice único → evita duplicados a nivel de BD.
- `created_at` y `updated_at` para auditoría básica.

---

## Paso 3. Nueva entidad para recuperación de contraseña

Crea el archivo `esiusuarios/src/main/java/edu/esi/ds/esiusuarios/model/PasswordResetToken.java`:

```java
package edu.esi.ds.esiusuarios.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Guardamos SOLO el hash del token, nunca el token en claro. */
    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PasswordResetToken() {}

    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public Instant getCreatedAt() { return createdAt; }
}
```

> **Por qué guardamos `token_hash` y no el token en claro:** si un atacante roba la BD, no debe poder usar los tokens de recuperación. Mismo principio que con la contraseña.

---

## Paso 4. DAOs (`dao/`)

### 4.1 `UserDao.java`

[esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dao/UserDao.java](esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dao/UserDao.java):

```java
package edu.esi.ds.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esiusuarios.model.User;

public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### 4.2 Nuevo `PasswordResetTokenDao.java`

Crea `esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dao/PasswordResetTokenDao.java`:

```java
package edu.esi.ds.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esiusuarios.model.PasswordResetToken;

public interface PasswordResetTokenDao extends JpaRepository<PasswordResetToken, Long> {
    // En este DAO no hace falta nada extra:
    // - heredamos save(), findAll(), delete()...
    // - findByTokenHash NO sirve porque BCrypt usa salt distinto por hash.
}
```

> Si en tu IDE quedan imports no usados (`Optional`, `User`), bórralos.

---

## Paso 5. DTOs (`dto/`)

Los DTO son objetos planos para entrada/salida HTTP. Mantienen la entidad aislada del exterior.

### 5.1 `RegisterRequest.java`

```java
package edu.esi.ds.esiusuarios.dto;

public class RegisterRequest {
    private String email;
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 5.2 `LoginRequest.java`

```java
package edu.esi.ds.esiusuarios.dto;

public class LoginRequest {
    private String email;
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 5.3 `LoginResponse.java`

```java
package edu.esi.ds.esiusuarios.dto;

public class LoginResponse {
    private String token;
    private long expiresInMs;

    public LoginResponse() {}
    public LoginResponse(String token, long expiresInMs) {
        this.token = token;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }
}
```

### 5.4 `ForgotPasswordRequest.java`

```java
package edu.esi.ds.esiusuarios.dto;

public class ForgotPasswordRequest {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

### 5.5 `ResetPasswordRequest.java`

```java
package edu.esi.ds.esiusuarios.dto;

public class ResetPasswordRequest {
    private String token;
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
```

---

## Paso 6. `JwtService` correcto (`services/JwtService.java`)

Reemplaza el contenido. Esta versión:
- Lee `secret` y `expiration-ms` de `application.properties`.
- Usa `Keys.hmacShaKeyFor(...)` (API recomendada de jjwt 0.11.x).
- Tiene `validateAndGetEmail(token)` para que `ExternalController` lo use.

```java
package edu.esi.ds.esiusuarios.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Devuelve el email guardado en el token, o lanza excepcion si es invalido. */
    public String validateAndGetEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("email", String.class);
    }

    public long getExpirationMs() { return expirationMs; }
}
```

> **Importante:** la clave (`app.jwt.secret`) debe tener mínimo 32 bytes para HS256. Si pones algo más corto, jjwt lanzará excepción al arrancar.

---

## Paso 7. `PasswordPolicy` y `EmailService` (utilidades)

### 7.1 Política de contraseñas

Crea `esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/PasswordPolicy.java`:

```java
package edu.esi.ds.esiusuarios.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PasswordPolicy {

    /**
     * Mínimo 8 caracteres, al menos 1 letra y 1 número.
     * Lanza 400 si no cumple.
     */
    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La contrasena debe tener al menos 8 caracteres.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La contrasena debe contener letras y numeros.");
        }
    }
}
```

### 7.2 Email de recuperación (envío real al correo del usuario)

Crea `esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/EmailService.java`:

```java
package edu.esi.ds.esiusuarios.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // JavaMailSender lo crea Spring automaticamente cuando spring.mail.host esta configurado.
    // required=false -> si no hay SMTP configurado, no rompe la app y caemos a consola.
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    /**
     * Envia el token de recuperacion al correo del usuario que lo solicito.
     * - Si SMTP esta configurado (spring.mail.* + app.mail.from), envia un email real.
     * - Si no, lo imprime por consola (modo demo / fallback para la defensa).
     */
    public void sendResetEmail(String to, String token) {
        String subject = "Recuperacion de contrasena - ESI Entradas";
        String body = "Hola,\n\n"
            + "Has solicitado restablecer tu contrasena en ESI Entradas.\n"
            + "Usa este token (valido durante unos minutos) en la pantalla de 'Restablecer contrasena':\n\n"
            + token + "\n\n"
            + "Si tu no has solicitado este cambio, ignora este correo.\n";

        if (mailSender == null || from == null || from.isBlank()) {
            // Fallback: SMTP no configurado -> log por consola.
            log.warn("SMTP no configurado (spring.mail.* / app.mail.from). Mostrando email por consola.");
            log.info("=== EMAIL RECUPERACION (modo consola) ===");
            log.info("Para:    {}", to);
            log.info("Asunto:  {}", subject);
            log.info("Cuerpo:\n{}", body);
            log.info("=========================================");
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email de recuperacion enviado a {}", to);
        } catch (Exception ex) {
            // No relanzamos: el endpoint /forgot-password debe seguir devolviendo 200
            // para no revelar si el email existe (anti user-enumeration).
            log.error("Fallo al enviar email a {}: {}", to, ex.getMessage());
        }
    }
}
```

**Por qué así:**
- `JavaMailSender` se inyecta como `required=false`: si en `application.properties` no rellenas `spring.mail.*`, la app **no peta**, simplemente cae al modo consola.
- El email se manda **al `to` que recibimos**, que en `UserService.forgotPassword` es `user.getEmail()` → es decir, **al correo del usuario que pidió el reset** (no a una cuenta fija).
- Capturamos cualquier excepción de SMTP y solo la logueamos: el endpoint `/forgot-password` **siempre** debe responder `200`, aunque el email falle, para no revelar si la cuenta existe (defensa anti *user enumeration*, OWASP).

---

## Paso 8. `UserService` reescrito (`services/UserService.java`)

Esta es la **clase central**. Quitamos todo el código en memoria (lista `users`, `login(name, password)`, `checkToken` en memoria) y dejamos solo lo persistente.

Reemplaza el contenido de [esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/UserService.java](esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/UserService.java):

```java
package edu.esi.ds.esiusuarios.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.dao.PasswordResetTokenDao;
import edu.esi.ds.esiusuarios.dao.UserDao;
import edu.esi.ds.esiusuarios.dto.ForgotPasswordRequest;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.LoginResponse;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
import edu.esi.ds.esiusuarios.dto.ResetPasswordRequest;
import edu.esi.ds.esiusuarios.model.PasswordResetToken;
import edu.esi.ds.esiusuarios.model.User;

@Service
public class UserService {

    @Autowired private UserDao userDao;
    @Autowired private PasswordResetTokenDao resetDao;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordPolicy passwordPolicy;
    @Autowired private EmailService emailService;

    @Value("${app.reset.expiration-minutes}")
    private long resetExpirationMinutes;

    private final SecureRandom random = new SecureRandom();

    // ---------------------------------------------------------------------
    // REGISTRO
    // ---------------------------------------------------------------------
    public void register(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email obligatorio.");
        }
        passwordPolicy.validate(req.getPassword());

        String email = req.getEmail().trim().toLowerCase();
        if (userDao.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya esta registrado.");
        }

        User user = new User(email, passwordEncoder.encode(req.getPassword()));
        userDao.save(user);
    }

    // ---------------------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------------------
    public LoginResponse login(LoginRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }
        User user = userDao.findByEmail(req.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Credenciales invalidas."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta desactivada.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }

        String jwt = jwtService.generateToken(user);
        return new LoginResponse(jwt, jwtService.getExpirationMs());
    }

    // ---------------------------------------------------------------------
    // VALIDACION DE TOKEN PARA SISTEMAS EXTERNOS
    // ---------------------------------------------------------------------
    public String checkToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token vacio.");
        }
        try {
            return jwtService.validateAndGetEmail(token);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido o expirado.");
        }
    }

    // ---------------------------------------------------------------------
    // RECUPERACION: solicitar token
    // ---------------------------------------------------------------------
    public void forgotPassword(ForgotPasswordRequest req) {
        // No revelamos si el email existe (evita user enumeration).
        Optional<User> opt = userDao.findByEmail(req.getEmail().trim().toLowerCase());
        if (opt.isEmpty()) {
            return;
        }
        User user = opt.get();

        // 1. Generar token aleatorio fuerte (32 bytes -> base64).
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        // 2. Guardar SOLO el hash en BD.
        String tokenHash = passwordEncoder.encode(plainToken);
        Instant expiresAt = Instant.now().plus(resetExpirationMinutes, ChronoUnit.MINUTES);

        resetDao.save(new PasswordResetToken(user, tokenHash, expiresAt));

        // 3. Enviar al usuario el token EN CLARO (solo a su email).
        emailService.sendResetEmail(user.getEmail(), plainToken);
    }

    // ---------------------------------------------------------------------
    // RECUPERACION: aplicar nueva contrasena
    // ---------------------------------------------------------------------
    public void resetPassword(ResetPasswordRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token obligatorio.");
        }
        passwordPolicy.validate(req.getNewPassword());

        // BCrypt incluye salt por hash, asi que recorremos los tokens vigentes
        // y comparamos con passwordEncoder.matches.
        PasswordResetToken match = resetDao.findAll().stream()
            .filter(t -> !t.isUsed())
            .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
            .filter(t -> passwordEncoder.matches(req.getToken(), t.getTokenHash()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Token invalido, usado o expirado."));

        // Actualizar contrasena del usuario.
        User user = match.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userDao.save(user);

        // Marcar token como usado (un solo uso).
        match.setUsed(true);
        resetDao.save(match);
    }

    // ---------------------------------------------------------------------
    // CANCELAR CUENTA
    // ---------------------------------------------------------------------
    public void cancelAccount(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta token.");
        }
        String jwt = authHeader.substring(7);
        String email;
        try {
            email = jwtService.validateAndGetEmail(jwt);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido.");
        }
        User user = userDao.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        // Borrado logico: marcar como inactivo (mejor que delete fisico por integridad referencial).
        user.setActive(false);
        user.setUpdatedAt(Instant.now());
        userDao.save(user);
    }
}
```

**Comentarios didácticos:**
- `existsByEmail` → evita registrar duplicados. La unicidad la garantiza también la BD por el `unique=true`.
- `passwordEncoder.encode` → hash BCrypt con salt automático.
- En `forgotPassword` **no revelamos si el email existe**: respondemos 200 igualmente. Esto evita el ataque de *user enumeration*.
- El token de reset se genera con `SecureRandom` (no `Math.random`).
- Guardamos el **hash** del token, no el token, para que un atacante con acceso a la BD no pueda usarlos.

---

## Paso 9. Controladores HTTP

### 9.1 `UserController.java` (reemplazar)

[esiusuarios/src/main/java/edu/esi/ds/esiusuarios/http/UserController.java](esiusuarios/src/main/java/edu/esi/ds/esiusuarios/http/UserController.java):

```java
package edu.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esiusuarios.dto.ForgotPasswordRequest;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.LoginResponse;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
import edu.esi.ds.esiusuarios.dto.ResetPasswordRequest;
import edu.esi.ds.esiusuarios.services.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest req) {
        userService.register(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest req) {
        userService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req);
    }

    /**
     * Cancelar la cuenta (punto 4 del enunciado).
     * Requiere ir autenticado: enviamos el JWT en la cabecera Authorization.
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/me")
    public void cancelAccount(
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {
        userService.cancelAccount(authHeader);
    }
}
```

### 9.2 `ExternalController.java` (reemplazar)

Lo importante: que la ruta sea `/external/checktoken/{token}` (todo minúsculas, igual que en `esientradas-master`).

```java
package edu.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esiusuarios.services.UserService;

@RestController
@RequestMapping("/external")
public class ExternalController {

    @Autowired
    private UserService userService;

    @GetMapping("/checktoken/{token}")
    public String checkToken(@PathVariable String token) {
        // Devuelve el email del usuario duenyo del token.
        // Lanza 400 si esta vacio, 401 si es invalido o expiro.
        return userService.checkToken(token);
    }
}
```

---

## Paso 10. `SecurityConfig` (`security/SecurityConfig.java`)

Permitimos el acceso anónimo solo a las rutas que **deben** ser anónimas (no puedes loguearte si necesitas estar logueado para loguearte 🙂).

```java
package edu.esi.ds.esiusuarios.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable())
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth
              .requestMatchers(
                  "/users/register",
                  "/users/login",
                  "/users/forgot-password",
                  "/users/reset-password",
                  "/external/checktoken/**"
              ).permitAll()
              .anyRequest().authenticated()
          );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: incluye salt automatico y es resistente a fuerza bruta.
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS: el frontend Angular corre en otro puerto (por ejemplo 4200).
     * Sin esto el navegador bloquea las llamadas POST /users/login, etc.
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of("http://localhost:4200"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource src =
            new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", config);
        return src;
    }
}
```

Y en el `filterChain(...)`, justo después de `csrf(...)`, **añade** la línea `.cors(...)` para activar CORS:

```java
http
  .cors(c -> {})            // <-- usa el bean corsConfigurationSource() de arriba
  .csrf(csrf -> csrf.disable())
  .sessionManagement(...)
  ...
```

> El antiguo `JwtFilter` que tenías está vacío y **no hace falta** para esta guía: validamos el token cuando lo piden (`/external/checktoken/...`). Puedes borrarlo o dejarlo sin registrar.

---

## Paso 11. Lado `esientradas-master` (validar sesión al comprar)

### 11.1 `UsuariosService.java`

Reemplaza [esientradas-master/src/main/java/edu/esi/ds/esientradas/services/UsuariosService.java](esientradas-master/src/main/java/edu/esi/ds/esientradas/services/UsuariosService.java):

```java
package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UsuariosService {

    @Value("${app.esiusuarios.url:http://localhost:8081}")
    private String esiusuariosBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Devuelve el email del usuario si el token es valido.
     * Lanza 400 si el token esta vacio, 401 si no es valido.
     */
    public String checkToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token.");
        }

        String url = UriComponentsBuilder
            .fromHttpUrl(esiusuariosBaseUrl)
            .pathSegment("external", "checktoken", userToken)
            .toUriString();

        try {
            String userEmail = restTemplate.getForObject(url, String.class);
            if (userEmail == null || userEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido.");
            }
            return userEmail;
        } catch (HttpClientErrorException ex) {
            // 400 o 401 que devuelva esiusuarios. En Spring 6 getStatusCode() devuelve HttpStatusCode.
            throw new ResponseStatusException(ex.getStatusCode().value(), "Token no valido.", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No se pudo contactar con esiusuarios.", ex);
        }
    }
}
```

Y añade en `esientradas-master/src/main/resources/application.properties`:

```properties
app.esiusuarios.url=http://localhost:8081
```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
### 11.2 `ComprasController.java`

Reemplaza [esientradas-master/src/main/java/edu/esi/ds/esientradas/http/ComprasController.java](esientradas-master/src/main/java/edu/esi/ds/esientradas/http/ComprasController.java):

```java
package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.services.UsuariosService;

@RestController
@RequestMapping("/compras")
public class ComprasController {

    @Autowired
    private UsuariosService usuariosService;

    /**
     * Segun el diagrama de secuencia del PDF (mensaje 33: GET /compra + tokens abcd y 1234)
     * y el diagrama dibujado a mano ("comprar(tokenEntrada, tokenUsuario)"), la operacion
     * recibe DOS tokens:
     *   - tokenEntrada: el que devolvio esientradas al prerreservar (abcd en el PDF).
     *   - tokenUsuario: el JWT que devolvio esiusuarios al loguearse (1234 en el PDF).
     *
     * Si tokenUsuario es null o invalido -> rechazamos (no hay sesion valida).
     */
    @PutMapping("/comprar")
    public String comprar(
            @RequestParam String tokenEntrada,
            @RequestParam String tokenUsuario) {

        if (tokenUsuario == null || tokenUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta tokenUsuario (no hay sesion).");
        }
        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta tokenEntrada (no hay prerreserva).");
        }

        // Validar sesion contra esiusuarios.
        String userEmail = usuariosService.checkToken(tokenUsuario);

        // A partir de aqui sabes QUIEN compra (userEmail) y QUE entradas estan prerreservadas (tokenEntrada).
        // Esta guia no implementa el cobro: solo deja la sesion validada.
        return "Compra autorizada para " + userEmail + " sobre prerreserva " + tokenEntrada;
    }
}
```

---

## Paso 12. Pruebas manuales rápidas (curl o Postman)

Asume que `esiusuarios` corre en `:8081` y `esientradas-master` en `:8080`.

### 12.1 Registro

```bash
curl -X POST http://localhost:8081/users/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"ana@uclm.es\",\"password\":\"Secreta123\"}"
```

Esperado: `200 OK`. En la BD: fila en `users` con `password_hash` (no la contraseña en claro).

### 12.2 Login

```bash
curl -X POST http://localhost:8081/users/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"ana@uclm.es\",\"password\":\"Secreta123\"}"
```

Esperado: JSON con `token` (JWT) y `expiresInMs`.

### 12.3 Validar token desde el otro backend

```bash
curl http://localhost:8081/external/checktoken/EL_JWT_QUE_TE_DIO_LOGIN
```

Esperado: el email del usuario.
Casos KO:
- Token vacío → `400`.
- Token mal firmado → `401`.
- Token expirado → `401`.

### 12.4 Recuperación de contraseña

```bash
curl -X POST http://localhost:8081/users/forgot-password ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"ana@uclm.es\"}"
```

Mira la **consola** de `esiusuarios`: aparecerá el token de reset.

```bash
curl -X POST http://localhost:8081/users/reset-password ^
  -H "Content-Type: application/json" ^
  -d "{\"token\":\"EL_TOKEN_DE_LA_CONSOLA\",\"newPassword\":\"NuevaPwd9\"}"
```

Repite el paso → debe dar `401` ("usado").

### 12.5 Cancelar cuenta

```bash
curl -X DELETE http://localhost:8081/users/me ^
  -H "Authorization: Bearer EL_JWT"
```

En la BD el campo `active` del usuario debe pasar a `0/false`.
Un login posterior debe devolver `401` ("Cuenta desactivada").

### 12.6 Compra con sesión válida

```bash
curl -X PUT "http://localhost:8080/compras/comprar?tokenEntrada=abcd&tokenUsuario=EL_JWT"
```

- Sin `tokenUsuario` → `401`.
- `tokenUsuario` mal firmado → `401`.
- `tokenUsuario` válido y `tokenEntrada` presente → `200`.

---

## Paso 13. Frontend (capa mínima de sesión)

En `esife/esife/src/app/` ya tienes `compra/` y `espectaculos/`. Solo necesitas:

1. Un componente de **login** que llame a `POST /users/login` y guarde el JWT en `localStorage`.
2. Una llamada que envíe `userToken` cuando el usuario pulsa "comprar".
3. Si no hay token en `localStorage` al pulsar comprar, redirigir a `/login`.

Servicio Angular `auth.service.ts`:

```ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  login(email: string, password: string) {
    return this.http.post<{ token: string }>(
      'http://localhost:8081/users/login',
      { email, password }
    );
  }

  saveToken(token: string) { localStorage.setItem('jwt', token); }
  getToken(): string | null { return localStorage.getItem('jwt'); }
  logout() { localStorage.removeItem('jwt'); }
  isLogged(): boolean { return !!this.getToken(); }
}
```

En el flujo de compra:

```ts
const token = this.auth.getToken();
if (!token) { this.router.navigate(['/login']); return; }

// tokenEntrada lo obtienes de la respuesta de prerreserva previa (mensaje 20 del PDF).
this.http.put(
  `http://localhost:8080/compras/comprar?tokenEntrada=${tokenEntrada}&tokenUsuario=${token}`,
  {}
).subscribe({
  next: ok => console.log('Compra autorizada:', ok),
  error: err => console.error('Sesion invalida', err)
});
```

---

## Paso 14. Checklist de defensa

Cuando termines, debes poder demostrar punto por punto:

- [ ] La tabla `users` se crea sola en SQL Server al arrancar (`ddl-auto=update`).
- [ ] La tabla `password_reset_token` se crea con FK a `users`.
- [ ] No hay ninguna contraseña en claro en BD: solo `password_hash` (BCrypt empieza por `$2a$`).
- [ ] `POST /users/register` rechaza contraseñas <8, sin letra o sin número (`400`).
- [ ] `POST /users/login` con credenciales correctas devuelve un JWT firmado.
- [ ] `GET /external/checktoken/{jwt}` devuelve el email; con JWT inválido → `401`; vacío → `400`.
- [ ] `POST /users/forgot-password` genera un token, lo guarda **hasheado**, y "lo envía" (consola).
- [ ] `POST /users/reset-password` cambia la contraseña; un segundo intento con el mismo token → `401`.
- [ ] `PUT /compras/comprar` rechaza la compra sin `tokenUsuario` válido y la acepta con ambos tokens.
- [ ] `DELETE /users/me` con JWT válido marca la cuenta como `active=false` y bloquea futuros logins.
- [ ] `SecurityConfig` solo deja anónimas las 5 rutas listadas en el paso 10.
- [ ] CORS configurado: el frontend en `http://localhost:4200` puede llamar sin error de navegador.

---

## Paso 15. Resumen para la defensa (lo que tienes que saber explicar)

1. **Por qué BCrypt y no SHA-256 a secas**: BCrypt incluye *salt* y es lento → resiste fuerza bruta.
2. **Por qué guardamos el token de reset hasheado**: si roban la BD, no pueden usarlos.
3. **Por qué JWT entre microservicios**: `esientradas-master` no necesita acceder a la BD de usuarios; basta con que `esiusuarios` valide el token.
4. **Por qué `forgot-password` no revela si el email existe**: evita *user enumeration*.
5. **Por qué `STATELESS`**: con JWT no necesitamos sesión HTTP en el servidor → escala mejor.
6. **OWASP Top 10 cubiertos**:
   - A01 Broken Access Control → `SecurityConfig` con `anyRequest().authenticated()`.
   - A02 Cryptographic Failures → BCrypt + JWT firmado HS256 + secret externalizado.
   - A03 Injection → uso de JPA con parámetros (no concatenación SQL).
   - A07 Identification and Authentication Failures → política de contraseñas + tokens de un solo uso con expiración.

---

## Apéndice A. Sobre los SQL `poblarBD.sql` y `generar_entradas_zona.sql`

Esos scripts pueblan la BD **MySQL de `esientradas`** (tablas `escenario`, `espectaculo`, `entrada`,
`de_zona`, `precisa`). **No tocan** la BD de `esiusuarios` (SQL Server con tablas `users` y
`password_reset_token`), que se crea sola por JPA con `ddl-auto=update`.

Son dos bases de datos **separadas** (lo confirma el diagrama del PDF: `<<SQL Server>> BD usuarios`
y `<<MySQL>> BD esientradas`). La comunicación entre los dos sistemas es **únicamente** vía HTTP
a través de `ExternalController` (`GET /external/checktoken/{token}`), nunca a través de la BD.

Por lo tanto, los cambios o datos de prueba en `poblarBD.sql` **no afectan** a esta guía: la guía
garantiza que cuando `esientradas` reciba una petición de compra, llamará a `esiusuarios` para
validar el JWT, sea cual sea el contenido de la BD de espectáculos.
