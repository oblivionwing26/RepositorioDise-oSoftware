package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dto.DtoCompra;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;
import jakarta.transaction.Transactional;

@Service
public class ComprasService {

    @Autowired
    private EntradaDao entradaDao;

    @Transactional
    public DtoCompra comprar(String tokenEntrada, String emailUsuario) {
        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de la entrada es requerido");
        }
        if (emailUsuario == null || emailUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no validado");
        }

        Entrada entrada = this.entradaDao.findByTokenPrerreservaForUpdate(tokenEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una prerreserva con ese token"));

        if (entrada.getEstado() != Estado.PRERRESERVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está prerreservada");
        }

        if (prerreservaExpirada(entrada)) {
            liberarPrerreserva(entrada);
            this.entradaDao.save(entrada);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva ha expirado");
        }

        if (!emailUsuario.equalsIgnoreCase(entrada.getUsuarioPrerreserva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva pertenece a otro usuario");
        }

        entrada.setEstado(Estado.VENDIDA);
        entrada.setTokenPrerreserva(null);
        entrada.setPrerreservaExpiraEn(null);
        entrada.setUsuarioPrerreserva(null);
        this.entradaDao.save(entrada);

        String mensaje = "Compra completada para el usuario: " + emailUsuario + " con la entrada: " + entrada.getId();
        return new DtoCompra(entrada.getId(), entrada.getPrecio(), entrada.getEstado().name(), emailUsuario, mensaje);
    }

    private boolean prerreservaExpirada(Entrada entrada) {
        return entrada.getPrerreservaExpiraEn() == null || !entrada.getPrerreservaExpiraEn().isAfter(LocalDateTime.now());
    }

    private void liberarPrerreserva(Entrada entrada) {
        entrada.setEstado(Estado.DISPONIBLE);
        entrada.setTokenPrerreserva(null);
        entrada.setPrerreservaExpiraEn(null);
        entrada.setUsuarioPrerreserva(null);
    }
}
