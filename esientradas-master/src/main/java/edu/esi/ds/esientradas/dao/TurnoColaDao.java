package edu.esi.ds.esientradas.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.esi.ds.esientradas.model.EstadoTurnoCola;
import edu.esi.ds.esientradas.model.TurnoCola;
import jakarta.persistence.LockModeType;

public interface TurnoColaDao extends JpaRepository<TurnoCola, Long> {

    Optional<TurnoCola> findFirstByIdEspectaculoAndEmailUsuarioAndEstadoInOrderByCreadoEnDesc(
        Long idEspectaculo,
        String emailUsuario,
        Collection<EstadoTurnoCola> estados
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TurnoCola t where t.id = :idTurno and t.emailUsuario = :emailUsuario")
    Optional<TurnoCola> findByIdAndEmailUsuarioForUpdate(
        @Param("idTurno") Long idTurno,
        @Param("emailUsuario") String emailUsuario
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select t from TurnoCola t
        where t.idEspectaculo = :idEspectaculo
          and t.estado in :estados
        order by t.creadoEn asc, t.id asc
        """)
    List<TurnoCola> findByIdEspectaculoAndEstadoInForUpdate(
        @Param("idEspectaculo") Long idEspectaculo,
        @Param("estados") Collection<EstadoTurnoCola> estados
    );
}