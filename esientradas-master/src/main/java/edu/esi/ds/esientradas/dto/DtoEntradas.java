package edu.esi.ds.esientradas.dto;


public class DtoEntradas {

    Integer total;
    Integer libres;
    Integer reservadas;
    Integer vendidas;
    public void setTotal(Integer total) {
        this.total = total;
    }
    public void setLibres(Integer libres) {
        this.libres = libres;
    }
    public void setReservadas(Integer reservadas) {
        this.reservadas = reservadas;
    }
    public void setVendidas(Integer vendidas) {
        this.vendidas = vendidas;
    }
    public Integer getTotal() {
        return total;
    }
    public Integer getLibres() {
        return libres;
    }
    public Integer getReservadas() {
        return reservadas;
    }
    public Integer getVendidas() {
        return vendidas;
    }




}
