package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.pbe.soda_caustica_flanges.model.StatusAlarme;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    @Modifying
    @Query("UPDATE Incidente i SET i.statusAlarme = :novo WHERE i.id = :id AND i.dataHora IS NOT NULL AND i.normalizadoEm IS NULL AND i.statusAlarme = :anterior")
    int marcarEmAtendimento(@Param("id") Long id, @Param("anterior") StatusAlarme anterior, @Param("novo") StatusAlarme novo);

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

    // Os eventos exibidos no painel devem ser limitados e incluir flange carregada.
    @EntityGraph(attributePaths = {"flange"})
    List<Incidente> findTop6ByDataHoraIsNotNullOrderByDataHoraDesc();

    @EntityGraph(attributePaths = {"flange"})
    List<Incidente> findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(Long unidadeId);

    // Busca somente IDs, para destacar todos os alarmes ativos sem trazer entidades inteiras.
    @Query("""
            SELECT DISTINCT i.flange.id FROM Incidente i
            WHERE i.dataHora IS NOT NULL AND i.normalizadoEm IS NULL
              AND i.flange IS NOT NULL
            """)
    List<Long> findIdsFlangesComAlertasAtivos();

    @Query("""
            SELECT DISTINCT i.flange.id FROM Incidente i
            WHERE i.dataHora IS NOT NULL AND i.normalizadoEm IS NULL
              AND i.flange.unidade.id = :unidadeId
            """)
    List<Long> findIdsFlangesComAlertasAtivosPorUnidade(@Param("unidadeId") Long unidadeId);

    long countByDataHoraIsNotNullAndNormalizadoEmIsNull();

    long countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(Long unidadeId);

    boolean existsByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(Long flangeId);

    boolean existsByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullAndIdNot(
            Long flangeId,
            Long incidenteId
    );
}