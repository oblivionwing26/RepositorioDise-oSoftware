package edu.esi.ds.esiusuarios.services;

import java.util.Date;

import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class JwtService {

    private String secret = "clave_super_secreta";

    public String generateToken(User user) {

        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .setExpiration(
                new Date(System.currentTimeMillis() + 86400000)
            )
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
}
