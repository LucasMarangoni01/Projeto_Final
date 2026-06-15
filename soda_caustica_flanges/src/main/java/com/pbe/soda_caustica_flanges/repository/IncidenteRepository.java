package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    // Busca todos os incidentes de um ano específico ordenado por mês
    List<Incidente> findByAnoOrderByMesAsc(Integer ano);

    // Retorna o mês com mais incidentes de um ano
    @Query("SELECT i FROM Incidente i WHERE i.ano = :ano ORDER BY i.quantidade DESC LIMIT 1")
    Incidente findPicoByAno(@Param("ano") Integer ano);

    // Total de incidentes de um ano
    @Query("SELECT SUM(i.quantidade) FROM Incidente i WHERE i.ano = :ano")
    Integer sumQuantidadeByAno(@Param("ano") Integer ano);
}