package edu.esi.ds.esientradas.dto;

import edu.esi.ds.esientradas.model.DeZona;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Precisa;

public class DtoEntradaDisponible {
    private Long id;
    private Long precio;
    private String estado;
    private String tipo;
    private Integer zona;
    private Integer fila;
    private Integer columna;
    private Integer planta;

    public static DtoEntradaDisponible from(Entrada entrada) {
        DtoEntradaDisponible dto = new DtoEntradaDisponible();
        dto.setId(entrada.getId());
        dto.setPrecio(entrada.getPrecio());
        dto.setEstado(entrada.getEstado().name());

        if (entrada instanceof DeZona deZona) {
            dto.setTipo("ZONA");
            dto.setZona(deZona.getZona());
        } else if (entrada instanceof Precisa precisa) {
            dto.setTipo("PRECISA");
            dto.setFila(precisa.getFila());
            dto.setColumna(precisa.getColumna());
            dto.setPlanta(precisa.getPlanta());
        } else {
            dto.setTipo("GENERAL");
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrecio() {
        return precio;
    }

    public void setPrecio(Long precio) {
        this.precio = precio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getZona() {
        return zona;
    }

    public void setZona(Integer zona) {
        this.zona = zona;
    }

    public Integer getFila() {
        return fila;
    }

    public void setFila(Integer fila) {
        this.fila = fila;
    }

    public Integer getColumna() {
        return columna;
    }

    public void setColumna(Integer columna) {
        this.columna = columna;
    }

    public Integer getPlanta() {
        return planta;
    }

    public void setPlanta(Integer planta) {
        this.planta = planta;
    }
}
