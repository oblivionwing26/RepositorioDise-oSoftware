package edu.esi.ds.esientradas.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

import edu.esi.ds.esientradas.EsientradasApplication;
import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.dao.EntradaDao;

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
        return this.entradaDao.countByEspectaculoIdAndEstado(idEspectaculo, Estado.DISPONIBLE);
    }

    public Integer getEntradasLibres(Long idEspectaculo) {
        return this.entradaDao.countByEspectaculoIdAndEstado(idEspectaculo, Estado.DISPONIBLE);
    }

    public DtoEntradas getNumeroDeEntradasComoDto(Long idEspectaculo) {
        Object o = this.entradaDao.getNumeroDeEntradasComoDto(idEspectaculo);
        Object[] arr = (Object[]) o;
        DtoEntradas dto = new DtoEntradas();
        dto.setTotal(((Number) arr[0]).intValue());
        dto.setLibres(((Number) arr[1]).intValue());
        dto.setReservadas(((Number) arr[2]).intValue());
        dto.setVendidas(((Number) arr[3]).intValue());        
        return dto;
    }

}
