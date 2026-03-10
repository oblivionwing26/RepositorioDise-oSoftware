package edu.esi.ds.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esiusuarios.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
public class ExternalController {
    @Autowired
    private UserService userService;

    @GetMapping("/checktoken/{token}")
    public String checkToken(@PathVariable String token){ 
        if (token == null || token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
        }
        String userName = this.userService.checkToken(token);
        if (userName == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no valido");            
        }
        return userName;
    }
}
