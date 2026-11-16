package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Manutencao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    long countByFlange_Unidade_Id(Long unidadeId);

    long countByFlange_Id(Long flangeId);

    long countByFuncionario_Id(Long funcionarioId);

    // Mantém compatibilidade com consultas históricas e relatórios.
    List<Manutencao> findByFlange_Unidade_IdOrderByDataDesc(Long unidadeId);

    List<Manutencao> findAllByOrderByDataDesc();

    // NULL representa registro legado anterior à coluna de arquivamento.
    @Query("""
        SELECT m
        FROM Manutencao m
        WHERE m.arquivada = false OR m.arquivada IS NULL
        ORDER BY m.data DESC
        """)
    List<Manutencao> findAtivasOrderByDataDesc();

    @Query("""
        SELECT m
        FROM Manutencao m
        WHERE m.flange.unidade.id = :unidadeId
          AND (m.arquivada = false OR m.arquivada IS NULL)
        ORDER BY m.data DESC
        """)
    List<Manutencao> findAtivasPorUnidadeOrderByDataDesc(
            @Param("unidadeId") Long unidadeId
    );

    List<Manutencao> findByArquivadaTrueOrderByArquivadaEmDesc();
}
