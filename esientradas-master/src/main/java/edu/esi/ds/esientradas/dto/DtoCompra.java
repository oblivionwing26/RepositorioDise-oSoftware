package edu.esi.ds.esientradas.dto;

public class DtoCompra {
    private Long idCompra;
    private Long idEntrada;
    private Long precio;
    private String estado;
    private String emailUsuario;
    private String codigoEntrada;
    private String referenciaPago;
    private String metodoPago;
    private String estadoPago;
    private boolean emailConfirmacionEnviado;
    private String mensaje;

    public DtoCompra() {
    }

    public DtoCompra(Long idCompra, Long idEntrada, Long precio, String estado, String emailUsuario,
            String codigoEntrada, String referenciaPago, String metodoPago, String estadoPago,
            boolean emailConfirmacionEnviado, String mensaje) {
        this.idCompra = idCompra;
        this.idEntrada = idEntrada;
        this.precio = precio;
        this.estado = estado;
        this.emailUsuario = emailUsuario;
        this.codigoEntrada = codigoEntrada;
        this.referenciaPago = referenciaPago;
        this.metodoPago = metodoPago;
        this.estadoPago = estadoPago;
        this.emailConfirmacionEnviado = emailConfirmacionEnviado;
        this.mensaje = mensaje;
    }

    public Long getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Long idCompra) {
        this.idCompra = idCompra;
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

    public String getCodigoEntrada() {
        return codigoEntrada;
    }

    public void setCodigoEntrada(String codigoEntrada) {
        this.codigoEntrada = codigoEntrada;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public boolean isEmailConfirmacionEnviado() {
        return emailConfirmacionEnviado;
    }

    public void setEmailConfirmacionEnviado(boolean emailConfirmacionEnviado) {
        this.emailConfirmacionEnviado = emailConfirmacionEnviado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
