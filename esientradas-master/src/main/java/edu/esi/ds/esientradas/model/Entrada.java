package edu.esi.ds.esientradas.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Entrada {
    @Id @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    protected Long id;
    private Long precio;     // Ojo: en céntimos de euro
    private String tokenPrerreserva;
    private LocalDateTime prerreservaExpiraEn;
    private String usuarioPrerreserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espectaculo_id", nullable = false)
    protected Espectaculo espectaculo;

    @Enumerated(EnumType.STRING)
    protected Estado estado;


    @OneToOne(fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL)
    @JoinColumn(name = "token_valor", referencedColumnName = "valor")
    protected Token token;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    @JsonIgnore
    public Espectaculo getEspectaculo() {
        return espectaculo;
    }
    public void setEspectaculo(Espectaculo espectaculo) {
        this.espectaculo = espectaculo;
    }
    public Estado getEstado() {
        return estado;
    }
    public void setEstado(Estado estado) {
        this.estado = estado;
    }
    public Long getPrecio() {
        return precio;
    }
    public void setPrecio(Long precio) {
        this.precio = precio;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
        if (token != null && token.getEntrada() != this) {
            token.setEntrada(this);
        }
    }
}
