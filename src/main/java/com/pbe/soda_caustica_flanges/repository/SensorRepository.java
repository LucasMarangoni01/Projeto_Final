package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Sensor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    @EntityGraph(attributePaths = {"flange", "flange.unidade"})
    List<Sensor> findAllByOrderByCodigoIdentificacaoAsc();

    @EntityGraph(attributePaths = {"flange", "flange.unidade"})
    @Override
    Optional<Sensor> findById(Long id);

    @EntityGraph(attributePaths = {"flange", "flange.unidade"})
    List<Sensor> findByFlange_IdIn(List<Long> flangeIds);

    boolean existsByCodigoIdentificacaoIgnoreCase(String codigoIdentificacao);

    boolean existsByCodigoIdentificacaoIgnoreCaseAndIdNot(
            String codigoIdentificacao,
            Long id
    );

    long countByFlange_Id(Long flangeId);
}
