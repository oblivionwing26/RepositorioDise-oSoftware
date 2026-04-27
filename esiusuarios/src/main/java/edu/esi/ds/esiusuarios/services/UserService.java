package edu.esi.ds.esiusuarios.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.dao.UserDao;
import edu.esi.ds.esiusuarios.dto.LoginRequest;
import edu.esi.ds.esiusuarios.dto.LoginResponse;
import edu.esi.ds.esiusuarios.dto.RegisterRequest;
import edu.esi.ds.esiusuarios.model.User;
import io.jsonwebtoken.Claims;

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JwtService jwtService;

    /* ====================== REGISTRO ====================== */

    public void register(RegisterRequest req) {
        if (req == null || req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email obligatorio.");
        }
        String email = req.getEmail().trim().toLowerCase();

        if (userDao.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email.");
        }

        // Requisito de seguridad: contrasena robusta
        PasswordPolicy.validate(req.getPassword());

        String hash = passwordEncoder.encode(req.getPassword());

        User user = new User(email, req.getName(), hash);
        userDao.save(user);
    }

    /* ====================== LOGIN ====================== */

    public LoginResponse login(LoginRequest req) {
        if (req == null || req.getEmail() == null || req.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }
        String email = req.getEmail().trim().toLowerCase();

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales invalidas."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta esta desactivada.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getName());
    }

    /* ============== VALIDACION DE TOKEN (para BE esientradas) ============== */

    /**
     * Devuelve el email asociado al token si es valido; null si no lo es.
     */
    public String checkToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Claims claims = jwtService.parse(token);
        if (claims == null) {
            return null;
        }
        return claims.get("email", String.class);
    }
}
