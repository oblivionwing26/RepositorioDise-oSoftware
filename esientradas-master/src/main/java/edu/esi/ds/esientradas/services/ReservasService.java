package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoPrerreserva;
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

    @Value("${app.prerreserva.expiration-minutes:10}")
    private long prerreservaExpirationMinutes;


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

    @Transactional
    public DtoPrerreserva prerreservar(Long idEntrada, String email) {
        if (idEntrada == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de entrada es requerido");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no validado");
        }

        Entrada entrada = this.entradaDao.findByIdForUpdate(idEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        LocalDateTime now = LocalDateTime.now();
        if (entrada.getEstado() == Estado.PRERRESERVADA && prerreservaExpirada(entrada, now)) {
            liberarPrerreserva(entrada);
        }

        if (entrada.getEstado() == Estado.VENDIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada ya está vendida");
        }
        if (entrada.getEstado() != Estado.DISPONIBLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está disponible");
        }

        String tokenEntrada = UUID.randomUUID().toString();
        LocalDateTime expiraEn = now.plusMinutes(prerreservaExpirationMinutes);

        entrada.setEstado(Estado.PRERRESERVADA);
        entrada.setTokenPrerreserva(tokenEntrada);
        entrada.setPrerreservaExpiraEn(expiraEn);
        entrada.setUsuarioPrerreserva(email);
        this.entradaDao.save(entrada);

        return new DtoPrerreserva(entrada.getId(), tokenEntrada, expiraEn, entrada.getPrecio());
    }

    @Scheduled(fixedRateString = "${app.prerreserva.cleanup-rate-ms:60000}")
    @Transactional
    public void liberarPrerreservasExpiradas() {
        this.entradaDao.liberarPrerreservasExpiradas(
            Estado.DISPONIBLE,
            Estado.PRERRESERVADA,
            LocalDateTime.now()
        );
    }

    private boolean prerreservaExpirada(Entrada entrada, LocalDateTime now) {
        return entrada.getPrerreservaExpiraEn() == null || !entrada.getPrerreservaExpiraEn().isAfter(now);
    }

    private void liberarPrerreserva(Entrada entrada) {
        entrada.setEstado(Estado.DISPONIBLE);
        entrada.setTokenPrerreserva(null);
        entrada.setPrerreservaExpiraEn(null);
        entrada.setUsuarioPrerreserva(null);
    }

}
