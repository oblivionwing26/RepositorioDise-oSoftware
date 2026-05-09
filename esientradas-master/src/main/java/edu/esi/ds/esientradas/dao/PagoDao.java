package edu.esi.ds.esientradas.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.Pago;

public interface PagoDao extends JpaRepository<Pago, Long> {
	Optional<Pago> findByTokenPago(String tokenPago);
}
