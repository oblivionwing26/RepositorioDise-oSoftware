package edu.esi.ds.esientradas.http;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.services.UsuariosService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/compras")
public class ComprasController {
    @Autowired
    private UsuariosService usuariosService;

    @PutMapping("/comprar")    
    public String comprar(HttpSession session, HttpServletResponse response, @RequestParam String userToken) {
        String sessionId = session.getId();
        if(userToken == null || userToken.isEmpty()) {
            try {
                response.sendRedirect("https://uclm.es/");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.usuariosService.checkToken(userToken);
    }
}