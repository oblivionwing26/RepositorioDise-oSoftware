package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.qos.logback.core.subst.Token;

import java.util.List;

import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.services.EscenarioService;
import edu.esi.ds.esientradas.services.ReservasService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/reservas")
public class ReservasController {
    private Entrada entrada;
    @Autowired
    private ReservasService service;

    @PutMapping("/reservar")
   public Long reservar(HttpSession session, @RequestParam Long idEntrada){
    Long precioEntrada = this.service.reservar(idEntrada, session.getId());

    
    Long precioTotal = (Long) session.getAttribute("precioTotal");
    if (precioTotal == null) {
        precioTotal = precioEntrada;
        session.setAttribute("precioTotal", precioTotal);
    }else {
        precioTotal += precioEntrada;
        session.setAttribute("precioTotal", precioTotal);
    }

    return precioTotal;
}

}

