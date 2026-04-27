package edu.esi.ds.esiusuarios.services;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Política de contraseña robusta (requisito de seguridad de la práctica):
 *  - Mínimo 10 caracteres
 *  - Al menos 1 minúscula, 1 mayúscula, 1 dígito y 1 carácter especial
 *  - Sin espacios en blanco
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 10;
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL =
            Pattern.compile(".*[!-/:-@\\[-`{-~].*");
    private static final Pattern WHITESPACE = Pattern.compile(".*\\s.*");

    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            reject("La contrasena debe tener al menos " + MIN_LENGTH + " caracteres.");
        }
        if (WHITESPACE.matcher(password).matches()) {
            reject("La contrasena no puede contener espacios en blanco.");
        }
        if (!LOWER.matcher(password).matches()) {
            reject("La contrasena debe contener al menos una letra minuscula.");
        }
        if (!UPPER.matcher(password).matches()) {
            reject("La contrasena debe contener al menos una letra mayuscula.");
        }
        if (!DIGIT.matcher(password).matches()) {
            reject("La contrasena debe contener al menos un digito.");
        }
        if (!SPECIAL.matcher(password).matches()) {
            reject("La contrasena debe contener al menos un caracter especial.");
        }
    }

    private static void reject(String msg) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
