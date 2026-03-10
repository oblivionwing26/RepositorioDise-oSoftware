package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.dto.DtoEspectaculo;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.services.BusquedaService;

@RestController
@RequestMapping("/busqueda")
@CrossOrigin(origins = "*")
public class BusquedaController {

    @Autowired
    private  BusquedaService service;

    @GetMapping("/getEscenarios")
    public List<Escenario> getEscenarios() {
        return this.service.getEscenarios();
    }

    @GetMapping("/saludar/{nombre}")
    public String saludar(@PathVariable String nombre, @RequestParam String apellido) {
        return "Hola, " + nombre + " " + apellido + ", esta es la búsqueda de entradas";
    }

    @GetMapping("/getEspectaculos")
    public List<DtoEspectaculo> getEspectaculos(@RequestParam String artista) {
        List<Espectaculo> espectaculos = this.service.getEspectaculos(artista);

        List<DtoEspectaculo> dtoEspectaculos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            return dto;
        }).toList();
        return dtoEspectaculos;
    }

        @GetMapping("/getEspectaculos/{idEscenario}")
    public List<DtoEspectaculo> getEspectaculosByEscenario(@PathVariable Long idEscenario) {
        List<Espectaculo> espectaculos = this.service.getEspectaculos(idEscenario);

        List<DtoEspectaculo> dtoEspectaculos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            return dto;
        }).toList();
        return dtoEspectaculos;
    }

    @GetMapping("/getNumeroDeEntradas/{idEspectaculo}")
    public Integer getEntradas(@PathVariable Long idEspectaculo) {
        return this.service.getNumeroDeEntradas(idEspectaculo);
    }

    @GetMapping("/getNumeroDeEntradasComoDto/{idEspectaculo}")
    public DtoEntradas getNumeroDeEntradasComoDto(@RequestParam Long idEspectaculo) {
        DtoEntradas dto = new DtoEntradas();
            return dto;
    }

    @GetMapping("/getEntradasLibres/{idEspectaculo}")
    public Integer getEntradasLibres(@PathVariable Long idEspectaculo) {
        return this.service.getEntradasLibres(idEspectaculo);
    }

    

}
