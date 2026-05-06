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

## Paso 13. Frontend (sesión completa con Angular)

> Estructura YA creada para ti en `esife/esife/src/app/`:
>
> ```
> login/                  (ya existía)
> register/               (recién creado)
> forgot-password/        (recién creado)
> reset-password/         (recién creado)
> services/auth.ts        (recién creado)
> compra/                 (ya existía)
> espectaculos/           (ya existía)
> ```
>
> Las rutas en `app.routes.ts` ya están añadidas (`/login`, `/register`, `/forgot-password`, `/reset-password`).
> Y `provideHttpClient(withFetch())` ya está en `app.config.ts`.
>
> A continuación tienes el código que debes pegar dentro de cada uno. Todos son **standalone components** (Angular 17+).

### 13.1 Servicio de autenticación — `src/app/services/auth.ts`

Centraliza llamadas al backend y la persistencia del JWT.

```ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface LoginResponse {
  token: string;
  expiresInMs: number;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly USERS_API = 'http://localhost:8081/users';
  private readonly STORAGE_KEY = 'jwt';

  constructor(private http: HttpClient) {}

  register(email: string, password: string): Observable<void> {
    return this.http.post<void>(`${this.USERS_API}/register`, { email, password });
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.USERS_API}/login`, { email, password });
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.USERS_API}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.USERS_API}/reset-password`, { token, newPassword });
  }

  cancelAccount(): Observable<void> {
    const headers = { Authorization: `Bearer ${this.getToken()}` };
    return this.http.delete<void>(`${this.USERS_API}/me`, { headers });
  }

  // --- Gestión local del token ---
  saveToken(token: string): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, token);
    }
  }
  getToken(): string | null {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(this.STORAGE_KEY) : null;
  }
  logout(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }
  isLogged(): boolean {
    return !!this.getToken();
  }
}
```

> **Por qué los `typeof localStorage !== 'undefined'`**: tu proyecto usa SSR (`main.server.ts`). En el servidor `localStorage` no existe; sin esa guarda el build SSR se rompe.

---

### 13.2 Login — `src/app/login/`

`login.ts`:

```ts
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  email = '';
  password = '';
  error: string | null = null;
  loading = false;

  constructor(private auth: Auth, private router: Router) {}

  submit(): void {
    this.error = null;
    this.loading = true;
    this.auth.login(this.email, this.password).subscribe({
      next: res => {
        this.auth.saveToken(res.token);
        this.loading = false;
        this.router.navigate(['/']);
      },
      error: err => {
        this.loading = false;
        this.error = err.status === 401
          ? 'Credenciales inválidas o cuenta desactivada.'
          : 'Error al iniciar sesión.';
      }
    });
  }
}
```

`login.html`:

```html
<section class="auth-card">
  <h2>Iniciar sesión</h2>

  <form (ngSubmit)="submit()" #f="ngForm">
    <label>
      Email
      <input type="email" name="email" [(ngModel)]="email" required />
    </label>

    <label>
      Contraseña
      <input type="password" name="password" [(ngModel)]="password" required />
    </label>

    <button type="submit" [disabled]="loading || !f.valid">
      {{ loading ? 'Entrando…' : 'Entrar' }}
    </button>
  </form>

  <p *ngIf="error" class="error">{{ error }}</p>

  <p class="links">
    <a routerLink="/register">Crear cuenta</a> ·
    <a routerLink="/forgot-password">¿Olvidaste tu contraseña?</a>
  </p>
</section>
```

`login.css` (mínimo):

```css
.auth-card { max-width: 360px; margin: 2rem auto; padding: 1.5rem; border: 1px solid #ddd; border-radius: 8px; }
.auth-card form { display: flex; flex-direction: column; gap: .75rem; }
.auth-card label { display: flex; flex-direction: column; font-size: .9rem; }
.auth-card input { padding: .5rem; }
.auth-card button { padding: .6rem; cursor: pointer; }
.auth-card .error { color: #c0392b; margin-top: .75rem; }
.auth-card .links { margin-top: 1rem; font-size: .85rem; }
```

---

### 13.3 Registro — `src/app/register/`

`register.ts`:

```ts
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  email = '';
  password = '';
  confirm = '';
  error: string | null = null;
  ok = false;
  loading = false;

  constructor(private auth: Auth, private router: Router) {}

  // Misma política que el backend (PasswordPolicy):
  // mínimo 8, al menos una letra y al menos un número.
  private validClient(p: string): boolean {
    return p.length >= 8 && /[A-Za-z]/.test(p) && /\d/.test(p);
  }

  submit(): void {
    this.error = null;
    this.ok = false;

    if (this.password !== this.confirm) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    if (!this.validClient(this.password)) {
      this.error = 'La contraseña debe tener mínimo 8 caracteres, al menos una letra y un número.';
      return;
    }

    this.loading = true;
    this.auth.register(this.email, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.ok = true;
        setTimeout(() => this.router.navigate(['/login']), 1200);
      },
      error: err => {
        this.loading = false;
        this.error = err.status === 409
          ? 'Ese email ya está registrado.'
          : err.status === 400
            ? 'Datos inválidos. Revisa email y contraseña.'
            : 'Error al registrar.';
      }
    });
  }
}
```

`register.html`:

```html
<section class="auth-card">
  <h2>Crear cuenta</h2>

  <form (ngSubmit)="submit()" #f="ngForm">
    <label>
      Email
      <input type="email" name="email" [(ngModel)]="email" required />
    </label>

    <label>
      Contraseña
      <input type="password" name="password" [(ngModel)]="password" required minlength="8" />
    </label>

    <label>
      Repite la contraseña
      <input type="password" name="confirm" [(ngModel)]="confirm" required />
    </label>

    <button type="submit" [disabled]="loading || !f.valid">
      {{ loading ? 'Creando…' : 'Registrarse' }}
    </button>
  </form>

  <p *ngIf="error" class="error">{{ error }}</p>
  <p *ngIf="ok" class="ok">Cuenta creada. Redirigiendo al login…</p>

  <p class="links">
    ¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a>
  </p>
</section>
```

`register.css`: copia el mismo CSS del login (o reutiliza con clases globales).

---

### 13.4 Olvidé mi contraseña — `src/app/forgot-password/`

`forgot-password.ts`:

```ts
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Auth } from '../services/auth';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPassword {
  email = '';
  sent = false;
  loading = false;

  constructor(private auth: Auth) {}

  submit(): void {
    this.loading = true;
    this.auth.forgotPassword(this.email).subscribe({
      next: () => { this.loading = false; this.sent = true; },
      // Por seguridad mostramos siempre el mismo mensaje (ver PasswordPolicy + UserService).
      error: () => { this.loading = false; this.sent = true; }
    });
  }
}
```

`forgot-password.html`:

```html
<section class="auth-card">
  <h2>Recuperar contraseña</h2>

  <form *ngIf="!sent" (ngSubmit)="submit()" #f="ngForm">
    <label>
      Email de tu cuenta
      <input type="email" name="email" [(ngModel)]="email" required />
    </label>
    <button type="submit" [disabled]="loading || !f.valid">
      {{ loading ? 'Enviando…' : 'Enviar enlace' }}
    </button>
  </form>

  <p *ngIf="sent">
    Si el email existe en nuestro sistema, te hemos enviado instrucciones para
    restablecer tu contraseña. Revisa tu bandeja de entrada y la carpeta de spam.
  </p>

  <p class="links"><a routerLink="/login">Volver al login</a></p>
</section>
```

> **Por qué el mismo mensaje exista o no el email**: defensa anti *user enumeration* (igual que en el backend).

---

### 13.5 Restablecer contraseña — `src/app/reset-password/`

El usuario llega aquí con un enlace `/reset-password?token=XXXX` (lo recibe por email, generado por `EmailService`).

`reset-password.ts`:

```ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Auth } from '../services/auth';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPassword implements OnInit {
  token = '';
  newPassword = '';
  confirm = '';
  error: string | null = null;
  ok = false;
  loading = false;

  constructor(private route: ActivatedRoute, private router: Router, private auth: Auth) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
  }

  private validClient(p: string): boolean {
    return p.length >= 8 && /[A-Za-z]/.test(p) && /\d/.test(p);
  }

  submit(): void {
    this.error = null;

    if (!this.token) { this.error = 'Falta el token de recuperación.'; return; }
    if (this.newPassword !== this.confirm) { this.error = 'Las contraseñas no coinciden.'; return; }
    if (!this.validClient(this.newPassword)) {
      this.error = 'La contraseña debe tener mínimo 8 caracteres, al menos una letra y un número.';
      return;
    }

    this.loading = true;
    this.auth.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.ok = true;
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: err => {
        this.loading = false;
        this.error = err.status === 401
          ? 'El token es inválido, ya se usó o ha expirado.'
          : 'No se pudo restablecer la contraseña.';
      }
    });
  }
}
```

`reset-password.html`:

```html
<section class="auth-card">
  <h2>Restablecer contraseña</h2>

  <form (ngSubmit)="submit()" #f="ngForm">
    <label>
      Token (lo recibiste por email)
      <input type="text" name="token" [(ngModel)]="token" required />
    </label>

    <label>
      Nueva contraseña
      <input type="password" name="newPassword" [(ngModel)]="newPassword" required minlength="8" />
    </label>

    <label>
      Repite la nueva contraseña
      <input type="password" name="confirm" [(ngModel)]="confirm" required />
    </label>

    <button type="submit" [disabled]="loading || !f.valid">
      {{ loading ? 'Guardando…' : 'Cambiar contraseña' }}
    </button>
  </form>

  <p *ngIf="error" class="error">{{ error }}</p>
  <p *ngIf="ok" class="ok">Contraseña actualizada. Redirigiendo al login…</p>
</section>
```

---

### 13.6 Integración con el flujo de compra

En `compra/compra.ts`, antes de hacer el `PUT /compras/comprar`, verifica que hay sesión y envía el `userToken` (= JWT de `Auth`):

```ts
// dentro del componente Compra
constructor(private http: HttpClient, private auth: Auth, private router: Router) {}

comprar(tokenEntrada: string): void {
  const userToken = this.auth.getToken();
  if (!userToken) {
    this.router.navigate(['/login']);
    return;
  }

  const url = `http://localhost:8080/compras/comprar`
            + `?tokenEntrada=${encodeURIComponent(tokenEntrada)}`
            + `&tokenUsuario=${encodeURIComponent(userToken)}`;

  this.http.put(url, {}, { responseType: 'text' }).subscribe({
    next: msg => console.log('Compra autorizada:', msg),
    error: err => {
      if (err.status === 401) {
        this.auth.logout();
        this.router.navigate(['/login']);
      } else {
        console.error('Error de compra', err);
      }
    }
  });
}
```

> El `tokenEntrada` (token de prerreserva, “abcd” en el PDF, mensaje 20) lo obtienes cuando el usuario hace clic en una entrada y `esientradas` la marca como prerreservada. Lo guardas en el componente y lo envías en el momento de comprar.

---

### 13.7 (Opcional pero recomendado) Botón Logout y enlace al Login

En `app.html` añade una pequeña barra:

```html
<nav style="display:flex; gap:1rem; padding:.5rem 1rem; background:#f5f5f5;">
  <a routerLink="/">Espectáculos</a>
  <span style="flex:1"></span>
  <ng-container *ngIf="!isLogged(); else logged">
    <a routerLink="/login">Login</a>
    <a routerLink="/register">Registro</a>
  </ng-container>
  <ng-template #logged>
    <button (click)="logout()">Cerrar sesión</button>
  </ng-template>
</nav>

<router-outlet />
```

Y en `app.ts`:

```ts
import { Auth } from './services/auth';
// ...
constructor(private auth: Auth, private router: Router) {}
isLogged() { return this.auth.isLogged(); }
logout() { this.auth.logout(); this.router.navigate(['/login']); }
```

---

### 13.8 Probar el frontend completo

```powershell
cd esife\esife
npm install
ng serve --open
```

Abre `http://localhost:4200`:

1. `/register` → crea una cuenta.
2. `/login` → entra. Mira `localStorage.jwt` en DevTools.
3. `/forgot-password` → mira la consola del backend `esiusuarios`: aparece el `RESET TOKEN` (o llega por SMTP si lo configuraste).
4. Visita `http://localhost:4200/reset-password?token=ESE_TOKEN` → cambia la contraseña.
5. Vuelve a `/login` y entra con la nueva contraseña.
6. Desde `/comprar`, simula la compra: si no hay token, te manda al login; si hay, va a `esientradas` que valida contra `esiusuarios`.

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

---

## Apéndice B. Archivos backend que tienes que crear ahora mismo (si aún no existen)

Estado detectado en tu proyecto al revisar la estructura: tu `services/UserService.java` ya
referencia `PasswordResetToken`, `PasswordResetTokenDao`, `ForgotPasswordRequest` y
`ResetPasswordRequest`. Si esas 4 clases no existen, **el módulo no compila**. Crea cada una
exactamente con este contenido (los pasos 3, 4.2, 5.4 y 5.5 que se referenciaban antes).

### B.1 `model/PasswordResetToken.java`

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/model/PasswordResetToken.java`

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

    @Column(name = "token_hash", nullable = false, length = 255)
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
    public void setId(Long id) { this.id = id; }

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

### B.2 `dao/PasswordResetTokenDao.java`

```java
package edu.esi.ds.esiusuarios.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esiusuarios.model.PasswordResetToken;

public interface PasswordResetTokenDao extends JpaRepository<PasswordResetToken, Long> {
    // Sin métodos extra: el reset usa findAll() + matches() porque BCrypt
    // genera un hash distinto cada vez (no se puede buscar por token_hash directo).
}
```

### B.3 `dto/ForgotPasswordRequest.java`

```java
package edu.esi.ds.esiusuarios.dto;

public class ForgotPasswordRequest {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

### B.4 `dto/ResetPasswordRequest.java`

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

### B.5 Verificación rápida tras crearlos

```powershell
cd esiusuarios
.\mvnw spring-boot:run
```

En el log debes ver:
- Hibernate creando `password_reset_token` (con FK a `users`).
- `Tomcat started on port(s): 8081`.

Si compila pero no arranca por error de constraint en SQL Server, suele ser que la tabla
`users` no tenía aún el `unique` en `email`. Borra esa tabla a mano en SSMS y vuelve a
arrancar para que Hibernate la recree limpia (solo la primera vez, mientras desarrollas).

---

## Apéndice C. Resumen de cambios aplicados al frontend

Ya hechos por ti / por el setup:

- ✅ `app.config.ts` con `provideHttpClient(withFetch())`.
- ✅ Componentes generados (vacíos, hay que pegar el código del paso 13):
  `login/`, `register/`, `forgot-password/`, `reset-password/`.
- ✅ `services/auth.ts` generado (vacío, hay que pegar el código del paso 13.1).
- ✅ `app.routes.ts` actualizado con las 4 rutas de autenticación.

Pendiente de tu lado:

- [ ] Pegar el código de cada componente en sus 3 archivos (`.ts`, `.html`, `.css`).
- [ ] Pegar el código del servicio `Auth` en `services/auth.ts`.
- [ ] (Opcional) Añadir la barra de navegación del paso 13.7 en `app.html` / `app.ts`.
- [ ] Modificar `compra.ts` para enviar `tokenUsuario` (paso 13.6).

---



SIGUIENTEPARTE

## Paso 16. Prerreserva de entradas

Objetivo: que el usuario no compre directamente una entrada libre, sino que primero la bloquee durante unos minutos con un token temporal.

### 16.1 Cambios de modelo en `esientradas`

En `Entrada`, añade campos para controlar el bloqueo temporal:

```java
private String estado; // LIBRE, PRERRESERVADA, VENDIDA
private String tokenPrerreserva;
private LocalDateTime prerreservaExpiraEn;
private String usuarioPrerreserva;
```

Reglas:

- Una entrada solo puede pasar de `LIBRE` a `PRERRESERVADA`.
- Una entrada `PRERRESERVADA` solo puede comprarse si el token coincide y no ha expirado.
- Si expira, vuelve automáticamente a `LIBRE`.

### 16.2 Endpoint de prerreserva

Crea un endpoint en `ReservasController`:

```java
@PutMapping("/prerreservar")
public Map<String, Object> prerreservar(
  @RequestParam Long idEntrada,
  @RequestParam String tokenUsuario
) {
  String email = usuariosService.checkToken(tokenUsuario);
  return reservasService.prerreservar(idEntrada, email);
}
```

Respuesta esperada:

```json
{
  "tokenEntrada": "abcd-1234",
  "expiraEn": "2026-05-05T19:35:00",
  "precio": 50
}
```


### 16.3 Servicio de prerreserva

En `ReservasService`, implementa:

1. Buscar entrada por `idEntrada`.
2. Si está `VENDIDA`, devolver `409 CONFLICT`.
3. Si está `PRERRESERVADA` y no ha expirado, devolver `409 CONFLICT`.
4. Si está `PRERRESERVADA` pero expirada, liberarla.
5. Generar `UUID.randomUUID().toString()` como `tokenEntrada`.
6. Guardar `estado = "PRERRESERVADA"`, `usuarioPrerreserva = email`, `prerreservaExpiraEn = now + 10 minutos`.
7. Devolver token, expiración y precio.

### 16.4 Liberación automática

Activa scheduling en la clase principal de `esientradas`:

```java
@EnableScheduling
```

Crea un servicio programado:

```java
@Scheduled(fixedRate = 60000)
public void liberarPrerreservasExpiradas() {
  // buscar entradas PRERRESERVADA con prerreservaExpiraEn < now
  // poner estado LIBRE y limpiar token/campos temporales
}
```

Esto evita que entradas bloqueadas se queden colgadas si el usuario abandona la compra.

---

## Paso 17. Compra definitiva usando prerreserva

Objetivo: que `/compras/comprar` use dos tokens:

- `tokenUsuario`: JWT de `esiusuarios`.
- `tokenEntrada`: token devuelto por `/reservas/prerreservar`.

### 17.1 Flujo backend

En `ComprasController`:

1. Validar `tokenUsuario` con `UsuariosService.checkToken(tokenUsuario)`.
2. Buscar entrada por `tokenEntrada`.
3. Verificar que está `PRERRESERVADA`.
4. Verificar que no está expirada.
5. Verificar que `usuarioPrerreserva` coincide con el email del JWT.
6. Ejecutar pago Stripe si está configurado.
7. Marcar entrada como `VENDIDA`.
8. Limpiar `tokenPrerreserva` y `prerreservaExpiraEn`.
9. Enviar email de confirmación con la entrada.

### 17.2 Estados de error recomendados

- `401 UNAUTHORIZED`: token de usuario inválido.
- `400 BAD_REQUEST`: falta `tokenEntrada` o `tokenUsuario`.
- `404 NOT_FOUND`: no existe prerreserva con ese token.
- `409 CONFLICT`: entrada expirada, vendida o prerreservada por otro usuario.
- `502 BAD_GATEWAY`: error al hablar con Stripe o con `esiusuarios`.

---

## Paso 18. Envío de email tras compra

Objetivo: cuando una compra termina correctamente, el usuario recibe un correo con los datos de la entrada.

### 18.1 Servicio de email en `esientradas`

Crea `CompraEmailService` con dos modos:

- Si `spring.mail.host` está configurado: enviar email real.
- Si no está configurado: imprimir el email por consola para pruebas.

Contenido mínimo:

```text
Asunto: Confirmacion de compra - ESI Entradas

Entrada comprada correctamente.
Artista: ...
Fecha: ...
Escenario: ...
Zona/Fila/Butaca: ...
Precio: ...
Codigo de entrada: ...
```

### 18.2 Cuándo enviarlo

Enviar el email solo después de:

1. Validar usuario.
2. Validar prerreserva.
3. Confirmar pago.
4. Guardar entrada como `VENDIDA`.

Si falla el email, no conviene deshacer la compra; registra el error y muestra al usuario que la compra fue correcta.

---

## Paso 19. Cola virtual / taquilla virtual

Objetivo: controlar picos de acceso cuando se abre la venta de un espectáculo.

### 19.1 Modelo recomendado

Crea una entidad `TurnoCola`:

```java
id
idEspectaculo
emailUsuario
estado // ESPERANDO, ACTIVO, EXPIRADO, FINALIZADO
posicion
creadoEn
activoHasta
```

### 19.2 Endpoints mínimos

En un `ColaController`:

```text
POST /cola/entrar?idEspectaculo&tokenUsuario
GET  /cola/estado/{idTurno}?tokenUsuario
POST /cola/finalizar/{idTurno}?tokenUsuario
```

### 19.3 Reglas

- Si la venta aún no está abierta, el usuario entra en `ESPERANDO`.
- Cuando llega su turno, pasa a `ACTIVO` durante, por ejemplo, 5 minutos.
- Solo usuarios con turno `ACTIVO` pueden prerreservar entradas de ese espectáculo.
- Si no actúa dentro del tiempo, el turno pasa a `EXPIRADO` y entra el siguiente.

### 19.4 Actualización en frontend

Primera versión sencilla: polling cada 5 segundos a `/cola/estado/{idTurno}`.

Versión mejorada: WebSocket para recibir la posición en tiempo real.

---

## Paso 20. Integración Stripe

Objetivo: preparar una pasarela de pago realista pero controlada para entorno académico.

### 20.1 Configuración

En `esientradas/src/main/resources/application.properties`:

```properties
stripe.secret-key=
stripe.currency=eur
```

Durante desarrollo, deja `stripe.secret-key` vacío y usa modo simulado.

### 20.2 Servicio de pagos

Crea `StripePaymentService`:

```java
public PaymentResult cobrar(Long cantidadCentimos, String descripcion, String tokenPago) {
  if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
    return PaymentResult.aprobado("SIMULADO");
  }

  // aqui va la llamada real a Stripe cuando tengas API key
}
```

### 20.3 Flujo recomendado

1. Frontend llama a `/reservas/prerreservar`.
2. Backend devuelve `tokenEntrada` y precio.
3. Frontend muestra resumen y pide confirmación.
4. Frontend llama a `/compras/comprar` con `tokenEntrada`, `tokenUsuario` y `tokenPago`.
5. Backend cobra con Stripe.
6. Backend marca entrada como `VENDIDA`.
7. Backend manda email de confirmación.

### 20.4 Prueba sin Stripe real

Mientras no haya API key:

- Aceptar `tokenPago=SIMULADO`.
- Registrar en consola: `Pago simulado aprobado`.
- Mantener el mismo flujo para que luego cambiar a Stripe real sea pequeño.

---

## Paso 21. Orden de implementación recomendado

1. Prerreserva sin cola y sin Stripe.
2. Compra definitiva con `tokenEntrada + tokenUsuario`.
3. Email post-compra en modo consola.
4. Vista frontend para seleccionar entrada y confirmar compra.
5. Cola virtual con polling.
6. Stripe simulado.
7. Stripe real si el profesor lo exige.
8. WebSocket para cola en tiempo real si queda margen.

No avances a Stripe hasta que prerreserva + compra + email funcionen con pruebas manuales.

---

## Memoria de trabajo y checklist global (modo vibecoding)

A partir de este punto cambiamos la forma de trabajo: la guía deja de ser un sitio donde pegar bloques grandes de código. La usaremos como memoria de lo que se va haciendo, decisiones tomadas, errores encontrados y checklist de las partes pendientes. El código lo iremos desarrollando directamente en el proyecto, respetando la estructura existente de paquetes: `model`, `dao`, `dto`, `services`, `http`, `security`, etc.

### Memoria reciente

- Reparado CORS en `esiusuarios`: el frontend SSR en `http://localhost:4000` ya puede llamar a `http://localhost:8081/users/register`.
- La causa era que Spring Security no tenia configurado `http.cors(...)`; el navegador bloqueaba el `OPTIONS` preflight antes de llegar al controller.
- Validado por consola:
  - `OPTIONS /users/register` desde `Origin: http://localhost:4000` devuelve `Access-Control-Allow-Origin: http://localhost:4000`.
  - `POST /users/register` con ese origen devuelve `200`.
- Validado desde navegador en `http://localhost:4000/register`: el formulario registra correctamente y redirige a `/login`.
- `esiusuarios` queda arrancado en el puerto `8081`.
- `esientradas` estaba validado previamente en el puerto `8080`.
- El frontend puede servirse con SSR en `http://localhost:4000`. `ng serve` sigue siendo inestable en esta maquina por memoria/Node, no por el codigo Angular.
- Fase de estabilizacion inmediata completada: `esientradas` tiene CORS global para `localhost:4000` y `localhost:4200`, los dos backends compilan con Maven y el frontend compila con `npm run build`.
- Fase de autenticacion y usuarios completada: eliminado `/users/login1`, registro/login usan SQL Server, JWT real, validacion de password/email, reset por token temporal, cancelacion por Bearer token y `/external/checktoken/{token}` devuelve el email del usuario autenticado.
- Se permitio `/error` en Spring Security para que los errores reales lleguen al cliente como `400`, `401` o `409`, en vez de quedar convertidos en `403`.

### Checklist de estabilizacion inmediata

- [x] Corregir CORS de `esiusuarios` para permitir `localhost:4000` y `localhost:4200`.
- [x] Confirmar que `/users/register` responde desde origen frontend.
- [x] Revisar CORS global de `esientradas` antes de conectar compra/prerreserva desde Angular.
- [x] Dejar documentado el comando estable para frontend: `npm run build` + `npm run serve:ssr:esife`.
- [ ] Revisar memoria virtual/archivo de paginacion de Windows para recuperar `ng serve` si se quiere usar modo desarrollo normal.

### Checklist de autenticacion y usuarios

- [x] Limpiar `UserController`: quitar endpoint legacy `/users/login1` si ya no se usa.
- [x] Hacer que todos los errores de login/registro devuelvan HTTP claros: `400`, `401`, `409`.
- [x] Añadir validacion de email y password en backend, no solo en Angular.
- [x] Asegurar que `users.email` sea unico en SQL Server.
- [x] Revisar el campo `active`: al registrar debe quedar activo por defecto.
- [x] Reconstruir o terminar flujo `forgot-password` y `reset-password` si se mantiene como requisito.
- [x] Crear/restaurar las clases necesarias para reset si se implementa: entidad de token, DAO y DTOs.
- [x] Implementar cancelacion de cuenta con JWT real.
- [x] Añadir tests manuales minimos: registro, login correcto, login incorrecto, email repetido.

### Checklist de JWT e integracion entre backends

- [x] Revisar `JwtService` para que tambien valide tokens y extraiga email, no solo genere tokens.
- [x] Cambiar `UserService.checkToken(...)` para validar JWT real en vez de usar usuarios de prueba en memoria.
- [x] Revisar `ExternalController`: debe exponer una ruta clara, por ejemplo `/external/checktoken/{token}`.
- [x] Confirmar que `esientradas` llama exactamente a esa ruta desde `UsuariosService`.
- [x] Probar compra con token JWT real generado por `/users/login`.
- [x] Evitar que `esientradas` conozca la base de datos de usuarios; solo debe llamar a `esiusuarios`.

### Checklist frontend de sesion

- [x] Probar `/register` desde navegador despues del fix de CORS.
- [x] Probar `/login` y confirmar que guarda `jwt` en `localStorage`.
- [x] Mostrar errores de backend en formularios de forma clara.
- [x] Proteger acciones de compra: si no hay JWT, redirigir a `/login`.
- [x] Revisar que los nombres de respuesta coinciden: Angular espera `token` y `expiresIn`.
- [ ] Unificar estilos de `login`, `register`, `forgot-password` y `reset-password`.

### Checklist de entradas y disponibilidad

- [x] Revisar modelo `Entrada` actual y estados existentes.
- [x] Definir estados finales: se mantiene `DISPONIBLE` como equivalente actual de `LIBRE`, se anade `PRERRESERVADA`, se conserva `RESERVADA` por compatibilidad y `VENDIDA` queda para compra final.
- [x] Añadir endpoints para listar entradas libres por espectaculo.
- [x] Diseñar vista de seleccion de entradas segun tipo de recinto: zona, fila, butaca, planta.
- [x] Evitar doble venta con transacciones o bloqueo pesimista/optimista.

### Checklist de prerreserva

- [ ] Crear flujo `prerreservar(idEntrada, tokenUsuario)`.
- [ ] Validar usuario llamando a `esiusuarios`.
- [ ] Generar `tokenEntrada` temporal.
- [ ] Guardar expiracion de prerreserva.
- [ ] Impedir prerreservar una entrada vendida o ya bloqueada.
- [ ] Liberar automaticamente prerreservas expiradas.
- [ ] Probar caso feliz y casos de conflicto.

### Checklist de compra definitiva

- [ ] Cambiar compra para exigir `tokenEntrada` y `tokenUsuario`.
- [ ] Validar que el token de entrada existe, no ha expirado y pertenece al usuario.
- [ ] Marcar entrada como `VENDIDA` solo al final del flujo.
- [ ] Registrar datos minimos de la compra.
- [ ] Devolver una respuesta util para Angular: estado, entrada comprada, precio y mensaje.
- [ ] Probar compra correcta, token expirado, token de otro usuario y entrada ya vendida.

### Checklist de email tras compra

- [ ] Crear servicio de email en `esientradas`.
- [ ] Modo consola si no hay SMTP configurado.
- [ ] Enviar confirmacion solo tras compra guardada.
- [ ] Incluir datos de espectaculo, escenario, ubicacion, precio y codigo de entrada.
- [ ] No deshacer compra si falla el email; registrar el error.

### Checklist de cola virtual

- [ ] Definir entidad de turno de cola.
- [ ] Crear endpoint para entrar en cola.
- [ ] Crear endpoint para consultar posicion/estado.
- [ ] Crear endpoint para finalizar o consumir turno.
- [ ] Permitir prerreserva solo con turno activo cuando el espectaculo requiera cola.
- [ ] Implementar primero con polling desde Angular.
- [ ] Valorar WebSocket al final si queda tiempo.

### Checklist de Stripe / pagos

- [ ] Implementar primero modo simulado con `tokenPago=SIMULADO`.
- [ ] Separar servicio de pagos de la logica de compra.
- [ ] Guardar referencia de pago simulada o real.
- [ ] Integrar Stripe real solo cuando prerreserva, compra y email funcionen.
- [ ] Mantener configuracion por properties para no subir claves al repositorio.

### Checklist de pruebas y entrega

- [ ] Preparar secuencia demo: registrar usuario, login, ver espectaculo, prerreservar, comprar, recibir email en consola.
- [ ] Añadir datos de prueba suficientes en MySQL.
- [ ] Revisar que SQL Server arranca y crea tablas limpias.
- [x] Preparar comandos de arranque para los tres servicios.
- [x] Anotar incidencias conocidas: `ng serve` puede fallar por memoria en esta maquina; SSR funciona.
- [ ] Antes de entregar, hacer prueba desde cero con bases vacias o documentar datos iniciales necesarios.

### Resumen de fase: estabilizacion inmediata

- Anadido CORS global en `esientradas` para los origenes del frontend (`localhost:4000`, `127.0.0.1:4000`, `localhost:4200`, `127.0.0.1:4200`).
- Saneado `ReservasController` para que el backend de entradas siga compilando antes de entrar en la fase de prerreserva.
- Corregido el spec de Angular del servicio de espectaculos para que importe `EspectaculosService` y el build completo vuelva a funcionar.
- Comandos estables: backend usuarios `mvnw spring-boot:run` en `esiusuarios`; backend entradas `mvnw spring-boot:run` en `esientradas-master`; frontend `npm run build` y `npm run serve:ssr:esife` en `esife/esife`.

### Resumen de fase: autenticacion y usuarios

- Reemplazado el login legacy en memoria por registro/login persistido en SQL Server con BCrypt, validacion de email/password, usuario activo por defecto y errores HTTP claros.
- `JwtService` ahora genera y valida JWT; `UserService.checkToken` comprueba JWT real, usuario existente y usuario activo.
- `users.email` queda protegido con la constraint unica `ux_users_email`; se verifico con `sqlcmd` que existe en SQL Server y que un segundo registro del mismo email devuelve `409`.
- Restaurados `forgot-password` y `reset-password` con token temporal hasheado en base de datos y fallback de email por consola.
- Implementada cancelacion de cuenta con `Authorization: Bearer ...`, dejando el usuario inactivo y bloqueando logins posteriores.
- Validaciones manuales realizadas: registro `200`, email repetido `409`, password debil `400`, login incorrecto `401`, login correcto con `expiresIn`, `/external/checktoken/{token}` `200`, reset completo `200`, cancelacion `200` y login posterior `401`.

### Resumen de fase: JWT, sesion y disponibilidad

- Confirmado que `esientradas` solo integra usuarios mediante HTTP contra `esiusuarios`: no tiene DAO ni datasource hacia SQL Server de usuarios.
- Probado flujo backend real: registro, `/users/login`, `/external/checktoken/{token}`, compra autorizada desde `esientradas` con JWT real y rechazo `401` con token invalido.
- Anadido listado de entradas disponibles con DTO seguro (`DtoEntradaDisponible`) y resumen de estados desde JPA, sin exponer entidades polimorficas al frontend.
- Corregido `/busqueda/getNumeroDeEntradasComoDto/{idEspectaculo}` para usar el path variable real y anadido `/busqueda/getEntradasDisponibles/{idEspectaculo}`.
- La seleccion de entradas en Angular carga escenarios, espectaculos, resumen y entradas libres; si no hay sesion guarda la seleccion, redirige a login y vuelve a `/comprar` tras guardar el JWT.
- Configurado el SSR de Angular con `allowedHosts` para `localhost` y `127.0.0.1`, evitando el fallback por proteccion SSRF al abrir `http://localhost:4000`.
- Validaciones realizadas: `mvnw clean compile` en `esiusuarios` y `esientradas-master`, `npm run build`, consulta real de disponibilidad (`total=1007`, `libres=1007`) y prueba en navegador hasta compra autorizada.
