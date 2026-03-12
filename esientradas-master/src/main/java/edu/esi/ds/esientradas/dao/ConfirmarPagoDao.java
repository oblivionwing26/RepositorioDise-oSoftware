package edu.esi.ds.esientradas.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.Confirmacion;

public interface ConfirmarPagoDao extends JpaRepository<Confirmacion, Long> {

}
