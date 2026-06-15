package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Flange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlangeRepository extends JpaRepository<Flange, Long> {

    // Busca por ID (convertido para string) ou Nome da Flange, ignorando maiúsculas/minúsculas
    @Query("SELECT f FROM Flange f WHERE CAST(f.id AS string) LIKE CONCAT('%', :termo, '%') OR LOWER(f.nome_flange) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Flange> buscarPorTermo(@Param("termo") String termo, Pageable pageable);
}