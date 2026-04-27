package edu.esi.ds.esientradas.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.Configuracion;

public interface ConfiguracionDao extends JpaRepository<Configuracion, Long> {
    Optional<Configuracion> findByClave(String clave);
}
