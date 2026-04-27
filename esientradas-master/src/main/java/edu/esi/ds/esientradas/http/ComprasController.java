package edu.esi.ds.esientradas.http;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.services.ComprasService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/compras")
@CrossOrigin(origins = "*")
public class ComprasController {
    @Autowired
    private ComprasService comprasService;

    @PutMapping("/comprar")
    public String comprar(HttpSession session, HttpServletResponse response,
                          @RequestParam(required = false) String tokenEntradas,
                          @RequestParam(required = false) String userToken) {
        if (userToken == null || userToken.isEmpty()) {
            try {
                response.sendRedirect("http://localhost:4200/login");
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al redirigir al login");
            }
            return null;
        }
        return this.comprasService.comprar(tokenEntradas, userToken);
    }
}