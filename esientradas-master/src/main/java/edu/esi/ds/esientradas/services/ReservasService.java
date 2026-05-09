package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.List;
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

    @Autowired
    private ColaService colaService;

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
    public DtoPrerreserva prerreservar(Long idEntrada, String email, String tokenPrerreservaActual) {
        if (idEntrada == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de entrada es requerido");
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no validado");
        }

        Entrada entrada = this.entradaDao.findByIdForUpdate(idEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        this.colaService.validarTurnoActivo(entrada.getEspectaculo().getId(), email, idTurno);

        LocalDateTime now = LocalDateTime.now();

        if (entrada.getEstado() == Estado.PRERRESERVADA && prerreservaExpirada(entrada, now)) {
            liberarPrerreserva(entrada);
            this.entradaDao.save(entrada);
        }

        String tokenEntrada = tokenPrerreservaActual;

        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            tokenEntrada = UUID.randomUUID().toString();
        } else {
            List<Entrada> entradasYaPrerreservadas =
                this.entradaDao.findByTokenPrerreservaForUpdate(tokenEntrada);

            if (entradasYaPrerreservadas.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva anterior no existe o ha expirado");
            }

            for (Entrada e : entradasYaPrerreservadas) {
                if (prerreservaExpirada(e, now)) {
                    liberarPrerreserva(e);
                    this.entradaDao.save(e);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva anterior ha expirado");
                }

                if (!email.equalsIgnoreCase(e.getUsuarioPrerreserva())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva pertenece a otro usuario");
                }
            }
        }

        if (entrada.getEstado() == Estado.PRERRESERVADA
            && tokenEntrada.equals(entrada.getTokenPrerreserva())) {

            List<Entrada> entradas = this.entradaDao.findByTokenPrerreserva(tokenEntrada);
            long precioTotal = entradas.stream().mapToLong(Entrada::getPrecio).sum();

            return new DtoPrerreserva(
                entrada.getId(),
                tokenEntrada,
                entrada.getPrerreservaExpiraEn(),
                precioTotal
            );
        }

        if (entrada.getEstado() == Estado.VENDIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada ya está vendida");
        }

        if (entrada.getEstado() != Estado.DISPONIBLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está disponible");
        }

        LocalDateTime expiraEn = now.plusMinutes(prerreservaExpirationMinutes);

        entrada.setEstado(Estado.PRERRESERVADA);
        entrada.setTokenPrerreserva(tokenEntrada);
        entrada.setPrerreservaExpiraEn(expiraEn);
        entrada.setUsuarioPrerreserva(email);
        this.entradaDao.save(entrada);

        List<Entrada> entradas = this.entradaDao.findByTokenPrerreserva(tokenEntrada);
        long precioTotal = entradas.stream().mapToLong(Entrada::getPrecio).sum();

        return new DtoPrerreserva(
            entrada.getId(),
            tokenEntrada,
            expiraEn,
            precioTotal
        );
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

    @Transactional
    public void liberarEntradaPrerreservada(String tokenEntrada, Long idEntrada, String email) {
        if (tokenEntrada == null || tokenEntrada.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de prerreserva es requerido");
        }

        Entrada entrada = this.entradaDao.findByIdForUpdate(idEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        if (entrada.getEstado() != Estado.PRERRESERVADA) {
            return;
        }

        if (!tokenEntrada.equals(entrada.getTokenPrerreserva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no pertenece a esta prerreserva");
        }

        if (!email.equalsIgnoreCase(entrada.getUsuarioPrerreserva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La prerreserva pertenece a otro usuario");
        }

        liberarPrerreserva(entrada);
        this.entradaDao.save(entrada);
    }

}
