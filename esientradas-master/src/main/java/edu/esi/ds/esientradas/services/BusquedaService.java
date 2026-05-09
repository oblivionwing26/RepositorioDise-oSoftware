package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoEntradaDisponible;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;

@Service
public class BusquedaService {

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private EscenarioDao escenarioDao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    public List<Escenario> getEscenarios() {
        return this.escenarioDao.findAll();
    }

    public List<Espectaculo> getEspectaculos(String artista) {
        return this.espectaculoDao.findByArtista(artista);
    }

    public List<Espectaculo> getEspectaculos(Long idEscenario) {
        return this.espectaculoDao.findByEscenarioId(idEscenario);
    }

    public Integer getNumeroDeEntradas(Long idEspectaculo) {
        return Math.toIntExact(this.entradaDao.countByEspectaculoId(idEspectaculo));
    }

    public Integer getEntradasLibres(Long idEspectaculo) {
        return Math.toIntExact(this.entradaDao.countByEspectaculoIdAndEstado(idEspectaculo, Estado.DISPONIBLE));
    }

    public DtoEntradas getNumeroDeEntradasComoDto(Long idEspectaculo) {
        DtoEntradas dto = new DtoEntradas();
        dto.setTotal(Math.toIntExact(this.entradaDao.countByEspectaculoId(idEspectaculo)));
        dto.setLibres(Math.toIntExact(this.entradaDao.countByEspectaculoIdAndEstado(idEspectaculo, Estado.DISPONIBLE)));
        dto.setReservadas(Math.toIntExact(this.entradaDao.countByEspectaculoIdAndEstadoIn(
            idEspectaculo,
            List.of(Estado.PRERRESERVADA, Estado.RESERVADA)
        )));
        dto.setVendidas(Math.toIntExact(this.entradaDao.countByEspectaculoIdAndEstado(idEspectaculo, Estado.VENDIDA)));
        return dto;
    }

    public List<DtoEntradaDisponible> getEntradasDisponibles(Long idEspectaculo) {
        return this.entradaDao.findByEspectaculoIdAndEstadoOrderById(idEspectaculo, Estado.DISPONIBLE)
            .stream()
            .map(DtoEntradaDisponible::from)
            .toList();
    }
}
