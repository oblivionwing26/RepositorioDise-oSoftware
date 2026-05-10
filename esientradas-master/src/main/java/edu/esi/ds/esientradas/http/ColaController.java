package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoTurnoCola;
import edu.esi.ds.esientradas.services.ColaService;
import edu.esi.ds.esientradas.services.UsuariosService;

@RestController
@RequestMapping("/cola")
public class ColaController {

    @Autowired
    private ColaService colaService;

    @Autowired
    private UsuariosService usuariosService;

    @PostMapping("/entrar")
    public DtoTurnoCola entrar(
            @RequestParam Long idEspectaculo,
            @RequestParam(required = false) String tokenUsuario,
            @RequestParam(required = false) String clienteId) {
        return this.colaService.entrar(idEspectaculo, resolverIdentidad(tokenUsuario, clienteId));
    }

    @GetMapping("/estado/{idTurno}")
    public DtoTurnoCola estado(
            @PathVariable Long idTurno,
            @RequestParam(required = false) String tokenUsuario,
            @RequestParam(required = false) String clienteId) {
        return this.colaService.consultarEstado(idTurno, resolverIdentidad(tokenUsuario, clienteId));
    }

    @PostMapping("/finalizar/{idTurno}")
    public DtoTurnoCola finalizar(
            @PathVariable Long idTurno,
            @RequestParam(required = false) String tokenUsuario,
            @RequestParam(required = false) String clienteId) {
        return this.colaService.finalizar(idTurno, resolverIdentidad(tokenUsuario, clienteId));
    }

    private String resolverIdentidad(String tokenUsuario, String clienteId) {
        if (tokenUsuario != null && !tokenUsuario.isBlank()) {
            return this.usuariosService.checkToken(tokenUsuario);
        }

        if (clienteId == null || clienteId.isBlank()) {
            return null;
        }

        return "anon:" + clienteId.trim();
    }
}