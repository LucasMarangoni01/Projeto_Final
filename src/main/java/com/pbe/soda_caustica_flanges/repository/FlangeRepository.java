package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Flange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlangeRepository
        extends JpaRepository<Flange, Long> {

    @Query("""
        SELECT f FROM Flange f
        WHERE (f.ativo = true OR f.ativo IS NULL)
          AND f.unidade.id = :unidadeId
        ORDER BY f.id ASC
        """)
    List<Flange> findAtivasPorUnidade(@Param("unidadeId") Long unidadeId);

    @Query("""
        SELECT f FROM Flange f
        WHERE (f.ativo = true OR f.ativo IS NULL)
        ORDER BY f.id ASC
        """)
    List<Flange> findAtivas();

    /**
     * Lista todas as flanges vinculadas à unidade, inclusive arquivadas.
     * Usado na tela administrativa para permitir consultar/desvincular
     * vínculos históricos sem reativar o ativo automaticamente.
     */
    @Query("""
        SELECT f FROM Flange f
        WHERE f.unidade.id = :unidadeId
        ORDER BY f.id ASC
        """)
    List<Flange> findByUnidadeIdOrderByIdAsc(@Param("unidadeId") Long unidadeId);


    @Query("""
        SELECT f FROM Flange f
        WHERE f.ativo = false
        ORDER BY f.desativadaEm DESC
        """)
    List<Flange> findArquivadas();

    List<Flange> findByUnidadeIsNullOrderByIdAsc();

    @Query("""
        SELECT f
        FROM Flange f
        WHERE f.unidade IS NULL OR f.unidade.id <> :unidadeId
        ORDER BY f.id ASC
        """)
    List<Flange> findForaDaUnidade(@Param("unidadeId") Long unidadeId);

    long countByUnidadeId(Long unidadeId);

    @Query("""
        SELECT f
        FROM Flange f
        WHERE LOWER(f.nomeFlange) LIKE LOWER(CONCAT('%', :termo, '%'))
           OR LOWER(f.localizacao) LIKE LOWER(CONCAT('%', :termo, '%'))
        """)
    List<Flange> buscarPorTermo(
            @Param("termo") String termo,
            Pageable pageable
    );

    @Query("""
        SELECT f
        FROM Flange f
        WHERE f.unidade.id = :unidadeId
          AND (
                LOWER(f.nomeFlange)
                LIKE LOWER(CONCAT('%', :termo, '%'))
                OR LOWER(f.localizacao)
                LIKE LOWER(CONCAT('%', :termo, '%'))
              )
        """)
    List<Flange> buscarPorTermoEUnidade(
            @Param("termo") String termo,
            @Param("unidadeId") Long unidadeId,
            Pageable pageable
    );
}