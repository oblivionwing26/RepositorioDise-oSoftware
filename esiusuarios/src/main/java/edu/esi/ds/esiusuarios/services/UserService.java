package edu.esi.ds.esiusuarios.services;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.dao.PasswordResetTokenDao;
import edu.esi.ds.esiusuarios.dao.UserDao;
import edu.esi.ds.esiusuarios.dto.ForgotPasswordRequest;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
import edu.esi.ds.esiusuarios.dto.ResetPasswordRequest;
import edu.esi.ds.esiusuarios.model.PasswordResetToken;
import edu.esi.ds.esiusuarios.model.User;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordPolicy passwordPolicy;

    @Autowired
    private PasswordResetTokenDao passwordResetTokenDao;

    @Autowired
    private EmailService emailService;

    @Value("${app.reset.expiration-minutes:15}")
    private long resetExpirationMinutes;

    @Transactional
    public void register(RegisterRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de registro requeridos");
        }

        String email = normalizeEmail(req.getEmail());
        validateEmail(email);
        passwordPolicy.validate(req.getPassword());

        Optional<User> existingUser = userDao.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado");
            }

            // Si la cuenta estaba cancelada, la reactivamos
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            user.setActive(true);
            userDao.save(user);
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setActive(true);

        userDao.save(user);
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de login requeridos");
        }

        String email = normalizeEmail(req.getEmail());
        validateEmail(email);

        User user = userDao
            .findByEmail(email)
            .orElseThrow(() -> invalidCredentials());

        if (!user.isActive()) {
            throw invalidCredentials();
        }

        if (req.getPassword() == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return jwtService.generateToken(user);
    }

    @Transactional(readOnly = true)
    public String checkToken(String token) {
        String email = normalizeEmail(jwtService.validateAndGetEmail(token));
        User user = userDao
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario inactivo");
        }

        return user.getEmail();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        if (req == null || req.getEmail() == null || req.getEmail().isBlank()) {
            return;
        }

        String email = normalizeEmail(req.getEmail());
        userDao.findByEmail(email)
            .filter(User::isActive)
            .ifPresent(user -> {
                String token = generateResetToken();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setUser(user);
                resetToken.setTokenHash(passwordEncoder.encode(token));
                resetToken.setExpiresAt(Instant.now().plusSeconds(resetExpirationMinutes * 60));
                resetToken.setUsed(false);

                passwordResetTokenDao.save(resetToken);
                emailService.sendResetEmail(user.getEmail(), token);
            });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperacion requerido");
        }

        passwordPolicy.validate(req.getNewPassword());

        PasswordResetToken resetToken = findValidResetToken(req.getToken());
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setActive(true);
        resetToken.setUsed(true);

        userDao.save(user);
        passwordResetTokenDao.save(resetToken);
    }

    @Transactional
    public void cancelAccount(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        String email = checkToken(token);
        User user = userDao
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido"));

        user.setActive(false);
        userDao.save(user);
    }

    public long getJwtExpirationMs() {
        return jwtService.getExpirationMs();
    }

    private PasswordResetToken findValidResetToken(String rawToken) {
        List<PasswordResetToken> candidates = passwordResetTokenDao.findByUsedFalseAndExpiresAtAfter(Instant.now());
        return candidates.stream()
            .filter(candidate -> passwordEncoder.matches(rawToken, candidate.getTokenHash()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperacion no valido o expirado"));
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email no valido");
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales no validas");
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token bearer requerido");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
