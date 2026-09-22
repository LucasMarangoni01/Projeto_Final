package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Flange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlangeRepository
        extends JpaRepository<Flange, Long> {

    List<Flange> findByUnidadeId(Long unidadeId);

    @Query("""
        SELECT f
        FROM Flange f
        WHERE LOWER(f.nome_flange) LIKE LOWER(CONCAT('%', :termo, '%'))
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
                LOWER(f.nome_flange)
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