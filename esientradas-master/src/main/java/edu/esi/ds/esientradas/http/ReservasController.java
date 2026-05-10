package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoPrerreserva;
import edu.esi.ds.esientradas.services.ReservasService;
import edu.esi.ds.esientradas.services.UsuariosService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/reservas")
public class ReservasController {
    @Autowired
    private ReservasService service;

    @Autowired
    private UsuariosService usuariosService;

    @PutMapping("/reservar")
    public Long reservar(HttpSession session, @RequestParam Long idEntrada) {
        Long precioEntrada = this.service.reservar(idEntrada, session.getId());
        Long precioTotal = (Long) session.getAttribute("precioTotal");

        if (precioTotal == null) {
            precioTotal = precioEntrada;
        } else {
            precioTotal += precioEntrada;
        }

        session.setAttribute("precioTotal", precioTotal);
        return precioTotal;
    }

    @PutMapping("/prerreservar")
    public DtoPrerreserva prerreservar(
            @RequestParam Long idEntrada,
            @RequestParam String tokenUsuario,
            @RequestParam(required = false) Long idTurno,
            @RequestParam(required = false) String tokenPrerreserva) {

        String email = this.usuariosService.checkToken(tokenUsuario);

        return this.service.prerreservar(idEntrada, email, idTurno, tokenPrerreserva);
    }

    @DeleteMapping("/prerreservar/{idEntrada}")
    public void liberarEntradaPrerreservada(
            @PathVariable Long idEntrada,
            @RequestParam String tokenUsuario,
            @RequestParam String tokenPrerreserva) {

        String email = this.usuariosService.checkToken(tokenUsuario);

        this.service.liberarEntradaPrerreservada(tokenPrerreserva, idEntrada, email);
    }
}
