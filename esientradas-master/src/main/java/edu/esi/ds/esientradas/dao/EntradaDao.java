package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.ModelAttribute;

import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;

public interface EntradaDao extends JpaRepository<Entrada, Long> {
    List<Entrada> findByEspectaculoId(Long idEspectaculo);

    @Query(value = "UPDATE Entrada e SET e.estado = :estado WHERE e.id = :idEntrada")
    @Modifying
    void updateEstado(@Param("idEntrada") Long idEntrada, @Param("estado") Estado estado);

    Integer countByEspectaculoIdAndEstado(Long idEspectaculo, Estado estado);

    @Query(value = """
        SELECT count(*) as total,
               sum(case when estado = 'DISPONIBLE' then 1 else 0 end) AS libres,
               sum(case when estado = 'RESERVADA' then 1 else 0 end) AS reservadas,
               sum(case when estado = 'VENDIDA' then 1 else 0 end) AS vendidas
        FROM entrada
        WHERE espectaculo_id = :idEspectaculo""", nativeQuery = true)
    DtoEntradas getNumeroDeEntradasComoDto(@Param("idEspectaculo") Long idEspectaculo);
}