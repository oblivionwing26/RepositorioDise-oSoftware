package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.services.UsuariosService;

import java.io.IOException;


@RestController
@RequestMapping("/compras")
public class ComprasController {

    @Autowired
    private UsuariosService usuariosService;

    @PutMapping("/comprar")
    public String comprar ( 
        @RequestParam String tokenEntrada,
        @RequestParam Strin tokenUsuario
    ){
        if (tokenUsuario == null || tokenUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token del usuario es requerido");
        }

        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de la entrada es requerido");
        }

        String userEmail = usuariosService.checkToken(tokenUsuario);
        if (userEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de usuario inválido");
        }

        return "Compra autorizada para el usuario: " + userEmail + " con la entrada: " + tokenEntrada;
    }

}