package edu.esi.ds.esientradas.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.tokenPrerreserva = :tokenEntrada")
    List<Entrada> findByTokenPrerreservaForUpdate(@Param("tokenEntrada") String tokenEntrada);

    @Modifying
    @Query("""
        update Entrada e
        set e.estado = :estadoDisponible,
            e.tokenPrerreserva = null,
            e.prerreservaExpiraEn = null,
            e.usuarioPrerreserva = null
        where e.estado = :estadoPrerreservada
          and e.prerreservaExpiraEn < :now
        """)
    int liberarPrerreservasExpiradas(
        @Param("estadoDisponible") Estado estadoDisponible,
        @Param("estadoPrerreservada") Estado estadoPrerreservada,
        @Param("now") java.time.LocalDateTime now
    );

    List<Entrada> findByTokenPrerreserva(String tokenPrerreserva);
}