# Guia de implementacion funcional v3 (solo BD + autenticacion)

## 1. Alcance estricto
Esta guia queda limitada unicamente a:
1. Base de datos de usuarios y tablas de soporte para autenticacion.
2. Registro, login e inicio de sesion con JWT.
3. Recuperacion de contrasena (solicitud + reseteo).
4. Seguridad de contrasena (hash, politicas minimas, validaciones).
5. Verificacion de sesion entre sistemas para permitir compra solo si el usuario esta autenticado.

Fuera de alcance en esta version:
- Pasarela de pago (Stripe o equivalente).
- Logica de negocio de compra no relacionada con autenticacion.
- Mejoras visuales de frontend no relacionadas con login/sesion.

## 2. Interconexion entre sistemas (segun diagramas)
Flujo obligatorio para validar sesion al comprar:
1. FE envia token al backend de entradas al iniciar compra.
2. Backend de entradas llama a esiusuarios mediante el ExternalController.
3. ExternalController valida el JWT y devuelve identidad de usuario.
4. Si el token no es valido o expiro, se rechaza la compra con 401.

Ruta de integracion objetivo:
- esiusuarios: GET /external/checktoken/{token}
- esientradas-master: servicio intermedio que consume esa ruta antes de continuar el flujo de compra.

Nota de nomenclatura importante:
- Usar una sola variante y mantenerla en ambos sistemas: checktoken (todo en minusculas) o checkToken (camelCase), pero igual en los dos lados.

## 3. Modelo de datos (solo autenticacion)

### 3.1 Tabla users (obligatoria)
Campos minimos:
- id (PK)
- email (unico, not null)
- password_hash (not null)
- active (not null)
- created_at (recomendado)
- updated_at (recomendado)

Reglas:
1. No guardar password en claro.
2. El email debe tener indice unico.
3. La autenticacion se hace por email + password_hash.

### 3.2 Tabla password_reset_token (obligatoria para recuperacion)
Campos minimos:
- id (PK)
- user_id (FK a users.id)
- token_hash (not null)
- expires_at (not null)
- used (not null)
- created_at (recomendado)

Reglas:
1. El token de recuperacion se almacena hasheado, nunca en claro.
2. Expiracion corta recomendada: 15 a 30 minutos.
3. Token de un solo uso (used = true tras reset exitoso).

### 3.3 Opcional recomendado: tabla auth_audit
Para trazabilidad de seguridad:
- user_id
- event_type (LOGIN_OK, LOGIN_FAIL, RESET_REQUEST, RESET_OK)
- ip
- created_at

## 4. Backend esiusuarios (autenticacion y recuperacion)

### 4.1 Entidad User
Debe eliminar campos heredados de demo en memoria (name/password/token en claro) y dejar solo campos persistentes reales.

Estado objetivo:
- email unico
- passwordHash
- active

### 4.2 Endpoints publicos minimos
Controlador de usuarios con estas rutas:
1. POST /users/register
2. POST /users/login
3. POST /users/forgot-password
4. POST /users/reset-password

Comportamiento esperado:
- register: crea usuario con password hasheada.
- login: valida credenciales y devuelve JWT.
- forgot-password: genera token temporal de recuperacion.
- reset-password: valida token y actualiza password_hash.

### 4.3 Endpoint para sistemas externos
ExternalController (en paquete de controladores para sistemas) con:
- GET /external/checktoken/{token}

Debe:
1. Rechazar token vacio (400).
2. Validar firma y expiracion JWT.
3. Devolver identificador util del usuario (email o userId).

### 4.4 Reglas de seguridad de contrasena
Minimo obligatorio:
1. Longitud minima 8.
2. Al menos una letra y un numero.
3. Hash con BCrypt/Argon2.
4. No reutilizar token de recuperacion.

Recomendado:
- Invalidar todas las sesiones tras reset de contrasena.

### 4.5 SecurityConfig (rutas)
Permitir anonimo solo en:
- /users/register
- /users/login
- /users/forgot-password
- /users/reset-password
- /external/checktoken/** (solo para comunicacion entre sistemas)

El resto autenticado.

## 5. Backend esientradas-master (solo validacion de sesion)

### 5.1 UsuariosService
Responsabilidad unica en esta guia:
- Recibir token de compra.
- Llamar a esiusuarios ExternalController.
- Si valida, devolver identidad.
- Si falla, lanzar 401.

### 5.2 ComprasController
Antes de cualquier accion de compra:
1. Exigir userToken.
2. Invocar UsuariosService.checkToken(userToken).
3. Continuar solo si la validacion es correcta.

Nota:
- Esta guia no define logica de cobro.
- Solo define la precondicion de sesion valida para comprar.

## 6. Frontend (solo capa de sesion)
Solo lo necesario para autenticacion:
1. Pantalla login.
2. Guardar token de sesion.
3. Envio de token al backend de entradas en la operacion de compra.
4. Si no hay token, redireccion a login.

Fuera de alcance:
- Flujo de pago y UI avanzada.

## 7. Pruebas funcionales (solo autenticacion + recuperacion)

### 7.1 Registro
- POST /users/register con email nuevo.
- Esperado: alta correcta en BD con password hasheada.

### 7.2 Login
- POST /users/login con credenciales validas.
- Esperado: JWT valido.

### 7.3 Check token externo
- GET /external/checktoken/{jwt}
- Esperado: identidad de usuario.

Casos KO:
1. JWT invalido -> 401.
2. JWT expirado -> 401.
3. Token vacio -> 400.

### 7.4 Recuperacion de contrasena
1. POST /users/forgot-password (email existente).
2. Generar token temporal y guardarlo hasheado en BD.
3. POST /users/reset-password con token valido + nueva password.
4. Verificar que token queda marcado como usado.

Casos KO:
1. Token de recuperacion expirado -> 400/401.
2. Token ya usado -> 400/401.
3. Password nueva que no cumple politica -> 400.

### 7.5 Validacion al comprar
- Enviar compra sin token -> 400/401.
- Enviar compra con token invalido -> 401.
- Enviar compra con token valido -> pasa la validacion de sesion.

## 8. Checklist de cierre (solo alcance pedido)

- [ ] La guia no incluye tareas de Stripe/pago no relacionadas con sesion.
- [ ] Existe modelo de BD para users y password_reset_token.
- [ ] Login genera JWT con expiracion.
- [ ] ExternalController valida token para consumo entre sistemas.
- [ ] esientradas-master bloquea compra si no hay sesion valida.
- [ ] Existe flujo completo de recuperacion de contrasena.
- [ ] Password se almacena solo hasheada y con politica minima.

## 9. Decision de defensa (resumen)
La implementacion queda centrada exclusivamente en seguridad de acceso:
- persistencia de identidad,
- autenticacion,
- sesion,
- recuperacion,
- y federacion de validacion entre microservicios para permitir compra solo con usuario autenticado.

---

# 10. Tutorial paso a paso con codigo

> A partir de aqui actuo como profesor. Cada apartado te dice **donde** crear el archivo (siguiendo la estructura ya existente del proyecto: `dao`, `dto`, `model`, `services`, `http`, `security`) y **que** poner dentro. Copia, lee y entiende: cada bloque de codigo lleva una explicacion antes y despues.
>
> Paquete base de `esiusuarios`: `edu.esi.ds.esiusuarios`
> Paquete base de `esientradas-master`: `edu.esi.ds.esientradas`

## 10.0 Vision rapida del flujo que vamos a construir

```
[Frontend Angular]
   |  POST /users/register  --> crea cuenta (password hasheada con BCrypt)
   |  POST /users/login     --> devuelve JWT
   |  POST /users/forgot-password --> genera token de reset (hasheado en BD)
   |  POST /users/reset-password  --> cambia password si el token vale
   |
   |  PUT /compras/comprar?userToken=JWT
   v
[esientradas-master]
   |  llama a:
   v
[esiusuarios] GET /external/checktoken/{jwt}
   --> valida firma + expiracion del JWT
   --> devuelve email del usuario, o 401
```

Reglas que NO vamos a romper:
1. La password nunca se guarda en claro.
2. El token de recuperacion nunca se guarda en claro (se guarda su hash).
3. La ruta entre backends es `/external/checktoken/{token}` en minusculas (igual en los dos lados).

---

## 10.1 Base de datos: tablas minimas en SQL Server (`DBUsuarios`)

Tu `application.properties` actual ya apunta a SQL Server con `ddl-auto=update`, asi que **Hibernate creara las tablas a partir de las entidades JPA**. Aun asi conviene saber el DDL equivalente para entenderlo y para los controles de seguridad que pide el enunciado.

Archivo opcional: `esiusuarios/src/main/resources/schema-reference.sql` (solo de referencia, no se ejecuta automaticamente).

```sql
-- Tabla de usuarios
CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    email           NVARCHAR(190) NOT NULL,
    password_hash   NVARCHAR(255) NOT NULL,
    active          BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX ux_users_email ON users(email);

-- Tabla de tokens de recuperacion de contrasena
CREATE TABLE password_reset_token (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_hash      NVARCHAR(255) NOT NULL,
    expires_at      DATETIME2 NOT NULL,
    used            BIT NOT NULL DEFAULT 0,
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX ix_prt_user ON password_reset_token(user_id);
```

**Por que asi:**
- `email` con indice unico => evita duplicados y acelera el login.
- `password_hash` en columna separada => deja claro que no hay password en claro.
- `token_hash` => guardamos hash del token, no el token. Asi, si te roban la BD, no pueden resetear cuentas.
- `expires_at` => permite expirar tokens (15-30 min).
- `used` => garantiza un solo uso.

> Si quieres tambien la tabla opcional `auth_audit`, sigue el mismo patron con columnas `user_id`, `event_type`, `ip`, `created_at`.

---

## 10.2 `model/User.java` — entidad limpia

Tu `User` actual arrastra campos de demo (`name`, `password`, `token`). Vamos a dejar **solo** lo que se persiste de verdad. Mapeamos a la tabla `users` y a sus columnas.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/model/User.java`

```java
package edu.esi.ds.esiusuarios.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "ux_users_email", columnNames = "email")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User() {}

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

**Importante**: al borrar `name`, `password` y `token` del modelo, tambien **borra** del `UserService` el bloque de demo (`this.users = List.of(new User("Pepe",...))` y los metodos que los usaban). Lo veras en el paso 10.6.

---

## 10.3 `model/PasswordResetToken.java` — entidad para recuperacion

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

---

## 10.4 `dao/` — repositorios JPA

Ya tienes `UserDao`. Lo dejamos asi (esta bien) y anadimos el de tokens.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dao/UserDao.java` (sin cambios funcionales):

```java
package edu.esi.ds.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esiusuarios.model.User;

public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dao/PasswordResetTokenDao.java` (nuevo):

```java
package edu.esi.ds.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esiusuarios.model.PasswordResetToken;

public interface PasswordResetTokenDao extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
```

**Por que buscamos por `tokenHash`** y no por el token original: cuando llega el token por la URL/email, lo hasheamos igual y lo buscamos. Asi nunca comparamos contra texto en claro guardado.

---

## 10.5 `dto/` — peticiones y respuestas

Ya tienes `LoginRequest`, `RegisterRequest`, `LoginResponse`. Anadimos los dos de recuperacion.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dto/ForgotPasswordRequest.java`:

```java
package edu.esi.ds.esiusuarios.dto;

public class ForgotPasswordRequest {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/dto/ResetPasswordRequest.java`:

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

## 10.6 `services/JwtService.java` — generar y validar JWT correctamente

Tu `JwtService` actual genera el token pero no lo valida y la `secret` esta cableada como String corto. Lo arreglamos: clave segura HS256 desde `application.properties`, expiracion configurable y metodo de validacion que devuelve el subject.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/JwtService.java`

```java
package edu.esi.ds.esiusuarios.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey key() {
        // HS256 requiere clave de al menos 256 bits (32 bytes).
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(key())
            .compact();
    }

    /**
     * Valida firma + expiracion. Devuelve los claims si todo ok.
     * Lanza excepcion (JwtException) si el token no es valido.
     */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
            .setSigningKey(key())
            .build()
            .parseClaimsJws(token);
        return jws.getBody();
    }

    public String getEmail(String token) {
        return parse(token).get("email", String.class);
    }
}
```

Anade en `esiusuarios/src/main/resources/application.properties`:

```properties
# JWT (la clave debe tener al menos 32 caracteres)
app.jwt.secret=CAMBIA_ESTA_CLAVE_POR_UNA_DE_AL_MENOS_32_CARACTERES_!!
app.jwt.expiration-ms=86400000
# Tokens de recuperacion (15 minutos)
app.reset.expiration-ms=900000
```

---

## 10.7 `services/UserService.java` — registro, login y recuperacion

Aqui aplicamos politica de password, hash con BCrypt, generacion de token de reset, y limpiamos los metodos de demo.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/UserService.java`

```java
package edu.esi.ds.esiusuarios.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

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

    // Politica minima: 8+ caracteres, al menos 1 letra y 1 numero.
    private static final Pattern PASSWORD_POLICY =
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final SecureRandom random = new SecureRandom();

    @Autowired private UserDao userDao;
    @Autowired private PasswordResetTokenDao resetDao;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EmailService emailService;

    @Value("${app.reset.expiration-ms:900000}")
    private long resetExpirationMs;

    // ---------- REGISTRO ----------
    public void register(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email obligatorio");
        }
        if (!PASSWORD_POLICY.matcher(req.getPassword() == null ? "" : req.getPassword()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La password debe tener al menos 8 caracteres, una letra y un numero");
        }
        if (userDao.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya registrado");
        }

        User u = new User();
        u.setEmail(req.getEmail().trim().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setActive(true);
        userDao.save(u);
    }

    // ---------- LOGIN ----------
    public LoginResponse login(LoginRequest req) {
        User user = userDao.findByEmail(
                req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta desactivada");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        String jwt = jwtService.generateToken(user);
        return new LoginResponse(jwt, user.getEmail(), user.getEmail());
    }

    // ---------- VALIDACION DE TOKEN PARA OTROS BACKENDS ----------
    public String checkToken(String token) {
        try {
            return jwtService.getEmail(token);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido");
        }
    }

    // ---------- FORGOT PASSWORD ----------
    public void forgotPassword(ForgotPasswordRequest req) {
        // Respondemos siempre 200 al controlador para no filtrar si el email existe.
        Optional<User> opt = userDao.findByEmail(
                req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase());
        if (opt.isEmpty()) return;

        User user = opt.get();
        String rawToken = generateRawToken();        // lo enviamos por email
        String tokenHash = passwordEncoder.encode(rawToken); // lo guardamos hasheado

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash(tokenHash);
        prt.setExpiresAt(Instant.now().plusMillis(resetExpirationMs));
        prt.setUsed(false);
        resetDao.save(prt);

        emailService.sendResetEmail(user.getEmail(), rawToken);
    }

    // ---------- RESET PASSWORD ----------
    public void resetPassword(ResetPasswordRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token obligatorio");
        }
        if (!PASSWORD_POLICY.matcher(req.getNewPassword() == null ? "" : req.getNewPassword()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La password debe tener al menos 8 caracteres, una letra y un numero");
        }

        // Recorremos los tokens no usados y comparamos con BCrypt.matches.
        // (En produccion: filtrar por user_id si lo conoces, o usar un id firmado en el token).
        PasswordResetToken match = resetDao.findAll().stream()
            .filter(t -> !t.isUsed())
            .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
            .filter(t -> passwordEncoder.matches(req.getToken(), t.getTokenHash()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido o expirado"));

        User user = match.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userDao.save(user);

        match.setUsed(true);
        resetDao.save(match);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

**Por que este diseno:**
- `register` valida politica de contrasena y unicidad de email.
- `login` no distingue entre "email no existe" y "password mal" => no filtra usuarios.
- `forgotPassword` no revela si el email esta registrado.
- `resetPassword` solo acepta token no usado y no expirado, y lo marca como `used = true`.

---

## 10.8 `services/EmailService.java` — envio del token

Para defensa basta con loggear el token (mock). Si activas SMTP real luego, solo cambias el cuerpo del metodo.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/services/EmailService.java`

```java
package edu.esi.ds.esiusuarios.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendResetEmail(String to, String rawToken) {
        // TODO: integrar con JavaMailSender / proveedor SMTP.
        log.info("[MOCK MAIL] Para: {}  Token de reset: {}", to, rawToken);
    }
}
```

---

## 10.9 `http/UserController.java` — endpoints publicos

Sustituye tu `UserController` por este. Quitamos `/login1` (era de la demo en memoria) y anadimos forgot/reset.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/http/UserController.java`

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
}
```

---

## 10.10 `http/ExternalController.java` — validacion entre backends

Tu `ExternalController` actual usa `/checktoken/{token}` en la raiz. La guia exige `GET /external/checktoken/{token}`. Lo movemos al prefijo correcto y lo conectamos a `UserService.checkToken`.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/http/ExternalController.java`

```java
package edu.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.services.UserService;

@RestController
@RequestMapping("/external")
public class ExternalController {

    @Autowired
    private UserService userService;

    @GetMapping("/checktoken/{token}")
    public String checkToken(@PathVariable String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
        }
        return userService.checkToken(token); // devuelve email o lanza 401
    }
}
```

---

## 10.11 `security/SecurityConfig.java` — rutas publicas

Permitimos anonimo solo lo que realmente debe ser publico.

`esiusuarios/src/main/java/edu/esi/ds/esiusuarios/security/SecurityConfig.java`

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
        return new BCryptPasswordEncoder();
    }
}
```

> Nota: `JwtFilter` en tu repo todavia esta vacio. Para el alcance actual no es obligatorio anadirlo a la cadena (la validacion la hace `ExternalController` via `JwtService.parse`). Si en el futuro proteges endpoints internos con JWT, registras el filtro en `SecurityConfig` con `.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`.

---

## 10.12 `esientradas-master`: validar sesion antes de comprar

### 10.12.1 `services/UsuariosService.java`

Tu version actual hace una llamada `getForObject(endpoint)` sin token (eso lanza error siempre) y mezcla URL `checkToken` con `checktoken`. Lo simplificamos y respetamos la ruta acordada.

`esientradas-master/src/main/java/edu/esi/ds/esientradas/services/UsuariosService.java`

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

    @Value("${app.esiusuarios.base-url:http://localhost:8081}")
    private String esiusuariosBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String checkToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
        }

        String url = UriComponentsBuilder
            .fromHttpUrl(esiusuariosBaseUrl)
            .path("/external/checktoken/{token}")
            .buildAndExpand(userToken)
            .toUriString();

        try {
            String email = restTemplate.getForObject(url, String.class);
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido");
            }
            return email;
        } catch (HttpClientErrorException ex) {
            // 400/401 que ya viene de esiusuarios -> lo propagamos como 401 al cliente.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al validar token externo", ex);
        }
    }
}
```

Anade en `esientradas-master/src/main/resources/application.properties`:

```properties
app.esiusuarios.base-url=http://localhost:8081
```

### 10.12.2 `http/ComprasController.java`

Quitamos el `sendRedirect` raro y exigimos token siempre.

`esientradas-master/src/main/java/edu/esi/ds/esientradas/http/ComprasController.java`

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

    @PutMapping("/comprar")
    public String comprar(@RequestParam String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta userToken");
        }
        String email = usuariosService.checkToken(userToken);

        // A partir de aqui ya hay sesion valida.
        // (Logica de compra real fuera del alcance de esta guia).
        return "Compra autorizada para: " + email;
    }
}
```

---

## 10.13 Frontend Angular (`esife`) — capa minima de sesion

Tres piezas: servicio de auth, guardar el JWT, y enviarlo en la compra.

### 10.13.1 `src/app/auth.service.ts`

```ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest { email: string; password: string; }
export interface LoginResponse { token: string; email: string; username: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly USERS_URL = 'http://localhost:8081/users';
  private readonly TOKEN_KEY = 'esi_token';

  constructor(private http: HttpClient) {}

  register(req: LoginRequest): Observable<void> {
    return this.http.post<void>(`${this.USERS_URL}/register`, req);
  }

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.USERS_URL}/login`, req).pipe(
      tap(res => localStorage.setItem(this.TOKEN_KEY, res.token))
    );
  }

  forgot(email: string): Observable<void> {
    return this.http.post<void>(`${this.USERS_URL}/forgot-password`, { email });
  }

  reset(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.USERS_URL}/reset-password`, { token, newPassword });
  }

  logout(): void { localStorage.removeItem(this.TOKEN_KEY); }
  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }
  isLoggedIn(): boolean { return !!this.getToken(); }
}
```

### 10.13.2 Componente de login (esquema minimo)

`src/app/login/login.component.ts`

```ts
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth.service';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  template: `
    <h2>Iniciar sesion</h2>
    <input [(ngModel)]="email"    placeholder="email" />
    <input [(ngModel)]="password" placeholder="password" type="password" />
    <button (click)="entrar()">Entrar</button>
    <p *ngIf="error" style="color:red">{{ error }}</p>
  `
})
export class LoginComponent {
  email = ''; password = ''; error = '';

  constructor(private auth: AuthService, private router: Router) {}

  entrar() {
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: () => this.router.navigate(['/espectaculos']),
      error: () => this.error = 'Credenciales invalidas'
    });
  }
}
```

### 10.13.3 Llamar a la compra adjuntando el token

`src/app/compra/compra.ts` (fragmento):

```ts
comprar() {
  const token = this.auth.getToken();
  if (!token) { this.router.navigate(['/login']); return; }

  const url = `http://localhost:8080/compras/comprar?userToken=${encodeURIComponent(token)}`;
  this.http.put<string>(url, {}).subscribe({
    next: msg => console.log(msg),
    error: err => {
      if (err.status === 401) this.router.navigate(['/login']);
      else console.error(err);
    }
  });
}
```

> Si quieres limpiar la URL, mete el token en cabecera `Authorization: Bearer <jwt>` y ajusta el backend para leerlo de cabecera. Para el alcance pedido, query param vale.

---

## 10.14 Como probarlo (curl / Postman)

Asume `esiusuarios` en `:8081` y `esientradas-master` en `:8080`.

```bash
# 1) Registro
curl -X POST http://localhost:8081/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"Ana12345"}'

# 2) Login -> guarda el token de la respuesta
curl -X POST http://localhost:8081/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com","password":"Ana12345"}'
# respuesta: {"token":"eyJhbGciOi...","email":"ana@test.com","username":"ana@test.com"}

# 3) Validacion entre backends
curl http://localhost:8081/external/checktoken/eyJhbGciOi...

# 4) Forgot -> mira la consola de esiusuarios para ver el token mock
curl -X POST http://localhost:8081/users/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@test.com"}'

# 5) Reset
curl -X POST http://localhost:8081/users/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"<token-del-log>","newPassword":"Ana99999"}'

# 6) Compra autorizada (esientradas-master)
curl -X PUT "http://localhost:8080/compras/comprar?userToken=eyJhbGciOi..."
```

Casos KO esperados:
- Login con password mal => 401.
- `/external/checktoken/xxx` con token invalido => 401.
- `/external/checktoken/` sin token => 400.
- Reset con token expirado o ya usado => 401.
- Compra sin token o con token invalido => 400 / 401.

---

## 10.15 Mapeo final con el checklist de la seccion 8

- [x] Modelo de BD para `users` y `password_reset_token` (10.1, 10.2, 10.3).
- [x] Login genera JWT con expiracion (10.6, 10.7).
- [x] `ExternalController` valida token entre sistemas en `/external/checktoken/{token}` (10.10).
- [x] `esientradas-master` bloquea compra si no hay sesion valida (10.12).
- [x] Flujo completo de recuperacion de contrasena (10.7, 10.8, 10.9).
- [x] Password almacenada solo hasheada con BCrypt y politica minima (10.7).
- [x] La guia no incluye logica de pago: solo la precondicion de sesion (10.12.2).

Con esto tienes, en el orden correcto, **todo lo que pide la guia funcional**: BD -> entidades -> repositorios -> DTO -> servicios (JWT, usuarios, email) -> controladores publicos -> controlador externo -> seguridad -> integracion en `esientradas-master` -> frontend Angular -> pruebas.
