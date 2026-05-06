package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Token;
import jakarta.transaction.Transactional;

@Service
public class ReservasService {

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private TokenDao tokenDao;


    @Transactional
    public Long reservar(Long idEntrada, String sessionId) {
        Entrada entrada = this.entradaDao.findByIdForUpdate(idEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        if (entrada.getEstado() != Estado.DISPONIBLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está disponible");
        }

        Token token = new Token();
        token.setEntrada(entrada);
        token.setSessionId(sessionId);
        this.tokenDao.save(token);

        entrada.setEstado(Estado.RESERVADA);
        this.entradaDao.save(entrada);

        return entrada.getPrecio();
    }

}
