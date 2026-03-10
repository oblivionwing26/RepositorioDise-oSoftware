package edu.esi.ds.esientradas.dto;

import java.time.LocalDateTime;

public class DtoEspectaculo {

    private String artista;
    private LocalDateTime fecha;
    private String escenario;
    public long id;

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setEscenario(String nombre) {
        this.escenario = nombre;
    }

    public String getArtista() {
        return artista;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getEscenario() {
        return escenario;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }   

}
