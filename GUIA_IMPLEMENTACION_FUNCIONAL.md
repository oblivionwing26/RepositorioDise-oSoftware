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
