package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dao.TurnoColaDao;
import edu.esi.ds.esientradas.dto.DtoTurnoCola;
import edu.esi.ds.esientradas.model.EstadoTurnoCola;
import edu.esi.ds.esientradas.model.TurnoCola;
import jakarta.transaction.Transactional;

@Service
public class ColaService {

    private static final List<EstadoTurnoCola> ESTADOS_ABIERTOS = List.of(
        EstadoTurnoCola.ESPERANDO,
        EstadoTurnoCola.ACTIVO
    );

    @Autowired
    private TurnoColaDao turnoColaDao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Value("${app.cola.enabled:true}")
    private boolean colaEnabled;

    @Value("${app.cola.active-minutes:5}")
    private long activeMinutes;

    @Value("${app.cola.active-slots:1}")
    private int activeSlots;

    @Transactional
    public DtoTurnoCola entrar(Long idEspectaculo, String emailUsuario) {
        validarEntrada(idEspectaculo, emailUsuario);
        String email = normalizarEmail(emailUsuario);

        if (!colaEnabled) {
            return DtoTurnoCola.sinCola(idEspectaculo, email);
        }

        mantenerCola(idEspectaculo, LocalDateTime.now());

        TurnoCola turno = this.turnoColaDao
            .findFirstByIdEspectaculoAndEmailUsuarioAndEstadoInOrderByCreadoEnDesc(idEspectaculo, email, ESTADOS_ABIERTOS)
            .orElseGet(() -> crearTurno(idEspectaculo, email));

        mantenerCola(idEspectaculo, LocalDateTime.now());
        turno = this.turnoColaDao.findById(turno.getId()).orElse(turno);
        return toDto(turno);
    }

    @Transactional
    public DtoTurnoCola consultarEstado(Long idTurno, String emailUsuario) {
        String email = normalizarEmail(emailUsuario);

        TurnoCola turno = this.turnoColaDao.findByIdAndEmailUsuarioForUpdate(idTurno, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno de cola no encontrado"));

        mantenerCola(turno.getIdEspectaculo(), LocalDateTime.now());
        turno = this.turnoColaDao.findById(turno.getId()).orElse(turno);
        return toDto(turno);
    }

    @Transactional
    public DtoTurnoCola finalizar(Long idTurno, String emailUsuario) {
        String email = normalizarEmail(emailUsuario);

        TurnoCola turno = this.turnoColaDao.findByIdAndEmailUsuarioForUpdate(idTurno, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno de cola no encontrado"));

        if (turno.getEstado() == EstadoTurnoCola.ESPERANDO || turno.getEstado() == EstadoTurnoCola.ACTIVO) {
            turno.setEstado(EstadoTurnoCola.FINALIZADO);
            turno.setPosicion(0);
            turno.setActivoHasta(null);
            this.turnoColaDao.save(turno);
        }

        mantenerCola(turno.getIdEspectaculo(), LocalDateTime.now());
        return toDto(turno);
    }

    @Transactional
    public void validarTurnoActivo(Long idEspectaculo, String emailUsuario, Long idTurno) {
        if (!colaEnabled) {
            return;
        }
        obtenerTurnoActivo(idEspectaculo, emailUsuario, idTurno);
    }

    @Transactional
    public void finalizarTurnoActivo(Long idEspectaculo, String emailUsuario, Long idTurno) {
        if (!colaEnabled || idTurno == null) {
            return;
        }

        TurnoCola turno = obtenerTurnoActivo(idEspectaculo, emailUsuario, idTurno);
        turno.setEstado(EstadoTurnoCola.FINALIZADO);
        turno.setPosicion(0);
        turno.setActivoHasta(null);
        this.turnoColaDao.save(turno);
        mantenerCola(idEspectaculo, LocalDateTime.now());
    }

    @Scheduled(fixedRateString = "${app.cola.cleanup-rate-ms:5000}")
    @Transactional
    public void mantenerColasAbiertas() {
        if (!colaEnabled) {
            return;
        }

        this.espectaculoDao.findAll()
            .forEach(espectaculo -> mantenerCola(espectaculo.getId(), LocalDateTime.now()));
    }

    private TurnoCola crearTurno(Long idEspectaculo, String emailUsuario) {
        TurnoCola turno = new TurnoCola();
        turno.setIdEspectaculo(idEspectaculo);
        turno.setEmailUsuario(emailUsuario);
        turno.setEstado(EstadoTurnoCola.ESPERANDO);
        turno.setPosicion(0);
        turno.setCreadoEn(LocalDateTime.now());
        return this.turnoColaDao.save(turno);
    }

    private TurnoCola obtenerTurnoActivo(Long idEspectaculo, String emailUsuario, Long idTurno) {
        if (idTurno == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debes entrar en la cola virtual antes de prerreservar");
        }

        String email = normalizarEmail(emailUsuario);
        mantenerCola(idEspectaculo, LocalDateTime.now());

        TurnoCola turno = this.turnoColaDao.findByIdAndEmailUsuarioForUpdate(idTurno, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Turno de cola no valido para este usuario"));

        if (!idEspectaculo.equals(turno.getIdEspectaculo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno pertenece a otro espectaculo");
        }

        if (turno.getEstado() != EstadoTurnoCola.ACTIVO || turno.getActivoHasta() == null
                || !turno.getActivoHasta().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno de cola no esta activo");
        }

        return turno;
    }

    private void mantenerCola(Long idEspectaculo, LocalDateTime now) {
        List<TurnoCola> turnos = this.turnoColaDao.findByIdEspectaculoAndEstadoInForUpdate(
            idEspectaculo,
            ESTADOS_ABIERTOS
        );

        int activos = 0;
        for (TurnoCola turno : turnos) {
            if (turno.getEstado() == EstadoTurnoCola.ACTIVO && turnoExpirado(turno, now)) {
                turno.setEstado(EstadoTurnoCola.EXPIRADO);
                turno.setPosicion(0);
                turno.setActivoHasta(null);
            }
            if (turno.getEstado() == EstadoTurnoCola.ACTIVO) {
                activos++;
            }
        }

        int posicion = 1;
        int huecosActivos = Math.max(1, activeSlots);
        for (TurnoCola turno : turnos) {
            if (turno.getEstado() != EstadoTurnoCola.ESPERANDO) {
                continue;
            }

            if (activos < huecosActivos) {
                turno.setEstado(EstadoTurnoCola.ACTIVO);
                turno.setPosicion(0);
                turno.setActivoHasta(now.plusMinutes(activeMinutes));
                activos++;
            } else {
                turno.setPosicion(posicion);
                posicion++;
            }
        }

        this.turnoColaDao.saveAll(turnos);
    }

    private boolean turnoExpirado(TurnoCola turno, LocalDateTime now) {
        return turno.getActivoHasta() == null || !turno.getActivoHasta().isAfter(now);
    }

    private DtoTurnoCola toDto(TurnoCola turno) {
        if (turno.getEstado() == EstadoTurnoCola.ACTIVO) {
            return DtoTurnoCola.from(turno, "Tu turno esta activo. Puedes prerreservar la entrada.");
        }
        if (turno.getEstado() == EstadoTurnoCola.ESPERANDO) {
            return DtoTurnoCola.from(turno, "Estas en cola. Espera a que tu turno pase a activo.");
        }
        if (turno.getEstado() == EstadoTurnoCola.EXPIRADO) {
            return DtoTurnoCola.from(turno, "Tu turno ha expirado. Entra de nuevo en la cola.");
        }
        return DtoTurnoCola.from(turno, "Turno finalizado.");
    }

    private void validarEntrada(Long idEspectaculo, String emailUsuario) {
        if (idEspectaculo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id del espectaculo es requerido");
        }
        if (emailUsuario == null || emailUsuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no validado");
        }
        if (!this.espectaculoDao.existsById(idEspectaculo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectaculo no encontrado");
        }
    }

    private String normalizarEmail(String emailUsuario) {
        return emailUsuario == null ? "" : emailUsuario.trim().toLowerCase(Locale.ROOT);
    }
}