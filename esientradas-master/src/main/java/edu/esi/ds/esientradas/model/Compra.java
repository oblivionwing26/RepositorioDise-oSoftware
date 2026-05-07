package edu.esi.ds.esientradas.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_id", nullable = false, unique = true)
    private Entrada entrada;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false, unique = true)
    private Pago pago;

    @Column(nullable = false)
    private String emailUsuario;

    @Column(nullable = false)
    private Long precioCentimos;

    @Column(nullable = false, unique = true)
    private String codigoEntrada;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String metodoPago;

    @Column(nullable = false)
    private String referenciaPago;

    @Column(nullable = false)
    private Long idEspectaculo;

    @Column(nullable = false)
    private String artista;

    @Column(nullable = false)
    private LocalDateTime fechaEspectaculo;

    @Column(nullable = false)
    private String escenario;

    @Column(nullable = false)
    private String ubicacion;

    @Column(nullable = false)
    private LocalDateTime compradaEn = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entrada getEntrada() {
        return entrada;
    }

    public void setEntrada(Entrada entrada) {
        this.entrada = entrada;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }

    public Long getPrecioCentimos() {
        return precioCentimos;
    }

    public void setPrecioCentimos(Long precioCentimos) {
        this.precioCentimos = precioCentimos;
    }

    public String getCodigoEntrada() {
        return codigoEntrada;
    }

    public void setCodigoEntrada(String codigoEntrada) {
        this.codigoEntrada = codigoEntrada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public Long getIdEspectaculo() {
        return idEspectaculo;
    }

    public void setIdEspectaculo(Long idEspectaculo) {
        this.idEspectaculo = idEspectaculo;
    }

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public LocalDateTime getFechaEspectaculo() {
        return fechaEspectaculo;
    }

    public void setFechaEspectaculo(LocalDateTime fechaEspectaculo) {
        this.fechaEspectaculo = fechaEspectaculo;
    }

    public String getEscenario() {
        return escenario;
    }

    public void setEscenario(String escenario) {
        this.escenario = escenario;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public LocalDateTime getCompradaEn() {
        return compradaEn;
    }

    public void setCompradaEn(LocalDateTime compradaEn) {
        this.compradaEn = compradaEn;
    }
}
