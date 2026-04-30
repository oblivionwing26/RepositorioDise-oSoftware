package edu.esi.ds.esiusuarios.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PasswordPolicy {
    //Las reglas que rigen la contraseña son:
    // - Al menos 8 caracteres de longitud.
    // - Al menos una letra 
    // - Al menos un número
    //Debe de lanzar 400 si no lo cumple (exception capturada y mensaje lanzado)

    public void validate (String password){
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres.");
        }
        // boolean hasLetter = false;
        // boolean hasDigit = false;
        // for (char c : password.toCharArray()) {
        //     if (Character.isLetter(c)) {
        //         hasLetter = true;
        //     } else if (Character.isDigit(c)) {
        //         hasDigit = true;
        //     }
        //     if (hasLetter && hasDigit) {
        //         break;
        //     }
        // }

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe contener al menos una letra y un número.");
        }


    }
}