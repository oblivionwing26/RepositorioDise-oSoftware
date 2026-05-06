package edu.esi.ds.esientradas.dto;

public class DtoCompra {
    private Long idEntrada;
    private Long precio;
    private String estado;
    private String emailUsuario;
    private String mensaje;

    public DtoCompra() {
    }

    public DtoCompra(Long idEntrada, Long precio, String estado, String emailUsuario, String mensaje) {
        this.idEntrada = idEntrada;
        this.precio = precio;
        this.estado = estado;
        this.emailUsuario = emailUsuario;
        this.mensaje = mensaje;
    }

    public Long getIdEntrada() {
        return idEntrada;
    }

    public void setIdEntrada(Long idEntrada) {
        this.idEntrada = idEntrada;
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

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
