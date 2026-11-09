package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    List<Funcionario> findByUnidadeIdOrderByNomeFuncionarioAsc(Long unidadeId);

    long countByUnidadeId(Long unidadeId);
}
