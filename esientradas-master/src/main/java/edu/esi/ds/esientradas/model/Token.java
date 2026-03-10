package edu.esi.ds.esientradas.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Token {
    @Id
    @Column(length = 36)
    private String valor;

    private Long hora;

    private String sessionId;

    @OneToOne(mappedBy = "token", fetch = FetchType.LAZY)
    @JsonIgnore
    private Entrada entrada;

    public Token() {
        this.valor = UUID.randomUUID().toString();
        this.hora = System.currentTimeMillis();
    }

    public String getValor() {
        return valor;
    }

    public Long getHora() {
        return hora;
    }

    public void setHora(Long hora) {
        this.hora = hora;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Entrada getEntrada() {
        return entrada;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setEntrada(Entrada entrada) {
        this.entrada = entrada;
        if (entrada != null && entrada.getToken() != this) {
            entrada.setToken(this);
        }
    }



public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
}
}
