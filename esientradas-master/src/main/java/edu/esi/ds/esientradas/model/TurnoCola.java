package edu.esi.ds.esientradas.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "turno_cola",
    indexes = {
        @Index(name = "idx_turno_cola_espectaculo_estado", columnList = "id_espectaculo,estado"),
        @Index(name = "idx_turno_cola_usuario", columnList = "email_usuario")
    }
)
public class TurnoCola {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_espectaculo", nullable = false)
    private Long idEspectaculo;

    @Column(name = "email_usuario", nullable = false, length = 254)
    private String emailUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurnoCola estado = EstadoTurnoCola.ESPERANDO;

    @Column(nullable = false)
    private Integer posicion = 0;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "activo_hasta")
    private LocalDateTime activoHasta;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public EstadoTurnoCola getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurnoCola estado) {
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
}