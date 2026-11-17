package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    // Busca todos os incidentes de um ano específico ordenado por mês
    List<Incidente> findByAnoOrderByMesAsc(Integer ano);

    // Retorna o mês com mais incidentes de um ano
    Incidente findFirstByAnoOrderByQuantidadeDesc(Integer ano);

    // Total de incidentes de um ano
    @Query("SELECT SUM(i.quantidade) FROM Incidente i WHERE i.ano = :ano")
    Integer sumQuantidadeByAno(@Param("ano") Integer ano);

    Optional<Incidente> findTopByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(
            Long flangeId
    );

    List<Incidente> findByDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc();

    List<Incidente> findByDataHoraIsNotNullOrderByDataHoraDesc();

    List<Incidente> findByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
            Long unidadeId
    );

    List<Incidente> findByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(
            Long unidadeId
    );

    long countByDataHoraIsNotNullAndNormalizadoEmIsNull();

    long countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(Long unidadeId);

    boolean existsByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(Long flangeId);

    boolean existsByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullAndIdNot(
            Long flangeId,
            Long incidenteId
    );
}