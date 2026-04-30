package edu.esi.ds.esiusuarios.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.dao.PasswordResetTokenDao;
import edu.esi.ds.esiusuarios.dao.UserDao;
import edu.esi.ds.esiusuarios.dto.ForgotPasswordRequest;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
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

    //Registro

    public void register(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email es obligatorio.");
        }
        passwordPolicy.validate(req.getPassword());

        String email = req.getEmail().toLowerCase();
        if(userDao.existsEmail(email)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está registrado.");
        }

        User user = new User(email, passwordEncoder.encode(req.getPassword()));
        userDao.save(user);
    }

    //Login
    public LoginResponse login (LoginRequest req){
        if (req.getEmail() == null || req.getEmail().isBBlank() || req.getPassword() == null || req.getPassword().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email y la contraseña son obligatorios.");
        }

        User user = userDao.findByEmail(req.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Credenciales invalidas."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta desactivada");

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }

        String jwt = jwtService.generateToken(user);
        return new LoginResponse (jwt, jwtService.getExpirationMs());
        }
    }


    //Validacion de token sist externos

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


    //Solicitar token de recuperacion de contraseña

    public void forgotPassword(ForgotPasswordRequest req) {
        // No revelamos si el email existe (evitar user enumeration)
        Optional<User> opt = userDao.findByEmail(req.getEmail().trim().toLowerCase());
        if (opt.isEmpty()) {
            return;
        }
        User user = opt.get();

        // 1. Generar token aleatorio fuerte (32 bytes > base64)
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        // 2. Guardar SOLO el hash en BD
        String tokenHash = passwordEncoder.encode(plainToken);
        Instant expiresAt = Instant.now().plus(resetExpirationMinutes, ChronoUnit.MINUTES);

        resetDao.save(new PasswordResetToken(user, tokenHash, expiresAt));

        // 3. Enviar al usuario el token EN CLARO (solo a su email)
        emailService.sendResetEmail(user.getEmail(), plainToken);
    }

    //Restablecer contraseña usando el token

    public void resetPassword(ResetPasswordRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token vacio.");
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

    //Cancelar cuenta

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
