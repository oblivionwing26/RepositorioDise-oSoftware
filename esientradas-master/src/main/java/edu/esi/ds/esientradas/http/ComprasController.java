package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dto.DtoCompra;
import edu.esi.ds.esientradas.services.ComprasService;
import edu.esi.ds.esientradas.services.UsuariosService;

@RestController
@RequestMapping("/compras")
public class ComprasController {

    @Autowired
    private UsuariosService usuariosService;

    @Autowired
    private ComprasService comprasService;

    @PutMapping("/comprar")
    public DtoCompra comprar ( 
        @RequestParam String tokenEntrada,
        @RequestParam String tokenUsuario,
        @RequestParam(required = false, defaultValue = "SIMULADO") String tokenPago) {
        if (tokenUsuario == null || tokenUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token del usuario es requerido");
        }

        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de la entrada es requerido");
        }

        String userEmail = usuariosService.checkToken(tokenUsuario);
        if (userEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de usuario inválido");
        }

        return this.comprasService.comprar(tokenEntrada, userEmail, tokenPago);
    }

}