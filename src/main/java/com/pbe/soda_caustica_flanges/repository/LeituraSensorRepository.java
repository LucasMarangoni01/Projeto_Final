package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.LeituraSensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeituraSensorRepository extends JpaRepository<LeituraSensor, Long> {

    Optional<LeituraSensor> findTopBySensor_IdOrderByRecebidaEmDesc(Long sensorId);

    Optional<LeituraSensor> findTopByFlange_IdOrderByRecebidaEmDesc(Long flangeId);
}
