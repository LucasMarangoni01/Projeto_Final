package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.TesteEmergencia;
import com.pbe.soda_caustica_flanges.model.StatusAlarme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TesteEmergenciaRepository extends JpaRepository<TesteEmergencia, Long> {
    List<TesteEmergencia> findByEstadoNotOrderByDetectadoEmDesc(StatusAlarme estado);
    List<TesteEmergencia> findTop20ByEstadoOrderByDetectadoEmDesc(StatusAlarme estado);
}
