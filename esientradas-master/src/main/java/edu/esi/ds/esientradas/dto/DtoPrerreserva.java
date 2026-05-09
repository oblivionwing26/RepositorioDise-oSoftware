package edu.esi.ds.esientradas.dto;

import java.time.LocalDateTime;

public class DtoPrerreserva {
    private Long idEntrada;
    private String tokenEntrada;
    private LocalDateTime expiraEn;
    private Long precio;

    public DtoPrerreserva() {
    }

    public DtoPrerreserva(Long idEntrada, String tokenEntrada, LocalDateTime expiraEn, Long precio) {
        this.idEntrada = idEntrada;
        this.tokenEntrada = tokenEntrada;
        this.expiraEn = expiraEn;
        this.precio = precio;
    }

    public Long getIdEntrada() {
        return idEntrada;
    }

    public void setIdEntrada(Long idEntrada) {
        this.idEntrada = idEntrada;
    }

    public String getTokenEntrada() {
        return tokenEntrada;
    }

    public void setTokenEntrada(String tokenEntrada) {
        this.tokenEntrada = tokenEntrada;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(LocalDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public Long getPrecio() {
        return precio;
    }

    public void setPrecio(Long precio) {
        this.precio = precio;
    }
}
