package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.LeituraSensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface LeituraSensorRepository extends JpaRepository<LeituraSensor, Long> {

    Optional<LeituraSensor> findTopBySensor_IdOrderByRecebidaEmDesc(Long sensorId);

    Optional<LeituraSensor> findTopByFlange_IdOrderByRecebidaEmDesc(Long flangeId);

    boolean existsByCodigoMensagem(String codigoMensagem);

    List<LeituraSensor> findByFlange_Unidade_IdOrderByRecebidaEmDesc(Long unidadeId);

    @EntityGraph(attributePaths = {"sensor", "flange"})
    List<LeituraSensor> findByFlange_IdInOrderByRecebidaEmDesc(List<Long> flangeIds);
}
