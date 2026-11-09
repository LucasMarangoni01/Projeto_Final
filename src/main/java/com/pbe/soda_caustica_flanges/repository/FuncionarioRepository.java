package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    List<Funcionario> findByUnidadeIdOrderByIdAsc(Long unidadeId);

    List<Funcionario> findByUnidadeIsNullOrderByIdAsc();

    @Query("""
            SELECT f
            FROM Funcionario f
            WHERE LOWER(f.nome_funcionario) = LOWER(:nome)
            """)
    List<Funcionario> buscarPorNomeExato(@Param("nome") String nome);

    long countByUnidadeId(Long unidadeId);
}
