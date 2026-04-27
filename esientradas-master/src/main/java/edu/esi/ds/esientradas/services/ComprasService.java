package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComprasService {

    @Autowired
    private UsuariosService usuariosService;

    @Autowired
    private PagosService pagosService;

    public String comprar(String tokenEntradas, String tokenUsuario) {
        if (tokenUsuario == null || tokenUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere autenticación");
        }

        // Validar token de usuario contra BE esiusuarios
        String userName = usuariosService.checkToken(tokenUsuario);

        // Delegar en PagosService para procesar el pago
        return "Usuario validado: " + userName + ". Proceda al pago.";
    }
}
