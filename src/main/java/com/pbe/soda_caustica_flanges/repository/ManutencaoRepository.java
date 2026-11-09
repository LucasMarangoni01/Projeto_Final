package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Manutencao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    long countByFlange_Unidade_Id(Long unidadeId);

    long countByFlangeId(Long flangeId);

    long countByFuncionarioId(Long funcionarioId);

    List<Manutencao> findByFlange_Unidade_IdOrderByDataDesc(Long unidadeId);

    List<Manutencao> findAllByOrderByDataDesc();
}
