package edu.esi.ds.esientradas.dto;

public class DtoPagoPreparado {
    private Long idPago;
    private Long centimos;
    private String moneda;
    private String metodo;
    private String estado;
    private String tokenPago;
    private String clientSecret;
    private String publicKey;

    public DtoPagoPreparado() {
    }

    public DtoPagoPreparado(Long idPago, Long centimos, String moneda, String metodo, String estado, String tokenPago, String clientSecret, String publicKey) {
        this.idPago = idPago;
        this.centimos = centimos;
        this.moneda = moneda;
        this.metodo = metodo;
        this.estado = estado;
        this.tokenPago = tokenPago;
        this.clientSecret = clientSecret;
        this.publicKey = publicKey;
    }

    public Long getIdPago() {
        return idPago;
    }

    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }

    public Long getCentimos() {
        return centimos;
    }

    public void setCentimos(Long centimos) {
        this.centimos = centimos;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTokenPago() {
        return tokenPago;
    }

    public void setTokenPago(String tokenPago) {
        this.tokenPago = tokenPago;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
