package edu.esi.ds.esientradas.dto;

import java.time.LocalDateTime;

import edu.esi.ds.esientradas.model.EstadoTurnoCola;
import edu.esi.ds.esientradas.model.TurnoCola;

public class DtoTurnoCola {
    private Long idTurno;
    private Long idEspectaculo;
    private String emailUsuario;
    private String estado;
    private Integer posicion;
    private LocalDateTime creadoEn;
    private LocalDateTime activoHasta;
    private boolean puedePrerreservar;
    private boolean colaActiva;
    private String mensaje;

    public static DtoTurnoCola from(TurnoCola turno, String mensaje) {
        DtoTurnoCola dto = new DtoTurnoCola();
        dto.setIdTurno(turno.getId());
        dto.setIdEspectaculo(turno.getIdEspectaculo());
        dto.setEmailUsuario(turno.getEmailUsuario());
        dto.setEstado(turno.getEstado().name());
        dto.setPosicion(turno.getPosicion());
        dto.setCreadoEn(turno.getCreadoEn());
        dto.setActivoHasta(turno.getActivoHasta());
        dto.setPuedePrerreservar(turno.getEstado() == EstadoTurnoCola.ACTIVO);
        dto.setColaActiva(true);
        dto.setMensaje(mensaje);
        return dto;
    }

    public static DtoTurnoCola sinCola(Long idEspectaculo, String emailUsuario) {
        DtoTurnoCola dto = new DtoTurnoCola();
        dto.setIdEspectaculo(idEspectaculo);
        dto.setEmailUsuario(emailUsuario);
        dto.setEstado(EstadoTurnoCola.ACTIVO.name());
        dto.setPosicion(0);
        dto.setPuedePrerreservar(true);
        dto.setColaActiva(false);
        dto.setMensaje("La cola virtual esta desactivada para este entorno.");
        return dto;
    }

    public Long getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(Long idTurno) {
        this.idTurno = idTurno;
    }

    public Long getIdEspectaculo() {
        return idEspectaculo;
    }

    public void setIdEspectaculo(Long idEspectaculo) {
        this.idEspectaculo = idEspectaculo;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getPosicion() {
        return posicion;
    }

    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getActivoHasta() {
        return activoHasta;
    }

    public void setActivoHasta(LocalDateTime activoHasta) {
        this.activoHasta = activoHasta;
    }

    public boolean isPuedePrerreservar() {
        return puedePrerreservar;
    }

    public void setPuedePrerreservar(boolean puedePrerreservar) {
        this.puedePrerreservar = puedePrerreservar;
    }

    public boolean isColaActiva() {
        return colaActiva;
    }

    public void setColaActiva(boolean colaActiva) {
        this.colaActiva = colaActiva;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}