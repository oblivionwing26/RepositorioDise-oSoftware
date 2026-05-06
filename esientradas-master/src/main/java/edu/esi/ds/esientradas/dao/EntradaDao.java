package edu.esi.ds.esientradas.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;

public interface EntradaDao extends JpaRepository<Entrada, Long> {
    List<Entrada> findByEspectaculoId(Long idEspectaculo);

    List<Entrada> findByEspectaculoIdAndEstadoOrderById(Long idEspectaculo, Estado estado);

    long countByEspectaculoId(Long idEspectaculo);

    long countByEspectaculoIdAndEstado(Long idEspectaculo, Estado estado);

    long countByEspectaculoIdAndEstadoIn(Long idEspectaculo, Collection<Estado> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.id = :idEntrada")
    Optional<Entrada> findByIdForUpdate(@Param("idEntrada") Long idEntrada);
}