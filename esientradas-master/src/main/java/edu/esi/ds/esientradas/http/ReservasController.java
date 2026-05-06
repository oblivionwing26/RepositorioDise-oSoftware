package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.services.ReservasService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/reservas")
public class ReservasController {
    @Autowired
    private ReservasService service;

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
}
