package edu.esi.ds.esiusuarios.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret:cambia_esta_clave_super_secreta_de_al_menos_32_caracteres!!}")
    private String secret;

    /** Validez del token en milisegundos. Por defecto 1 hora. */
    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * Valida la firma y la expiración del token.
     * Devuelve los claims si es válido o null si no lo es.
     */
    public Claims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
