package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.LeituraSensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeituraSensorRepository extends JpaRepository<LeituraSensor, Long> {

    Optional<LeituraSensor> findTopBySensor_IdOrderByRecebidaEmDesc(Long sensorId);

    Optional<LeituraSensor> findTopByFlange_IdOrderByRecebidaEmDesc(Long flangeId);

    boolean existsByCodigoMensagem(String codigoMensagem);

    List<LeituraSensor> findByFlange_Unidade_IdOrderByRecebidaEmDesc(Long unidadeId);


    Optional<LeituraSensor> findTopByOrderByRecebidaEmDesc();

    Optional<LeituraSensor> findTopByFlange_Unidade_IdOrderByRecebidaEmDesc(Long unidadeId);

    @EntityGraph(attributePaths = {"sensor", "flange"})
    List<LeituraSensor> findTop10ByFlange_IdInOrderByRecebidaEmDesc(List<Long> flangeIds);

    /**
     * Apenas a leitura mais recente por flange; histórico não é carregado inteiro.
     * Empates de horário são resolvidos pelo maior ID.
     */
    @EntityGraph(attributePaths = {"sensor", "flange"})
    @Query("""
            SELECT l FROM LeituraSensor l
            WHERE l.flange.id IN :flangeIds
              AND l.recebidaEm = (
                  SELECT MAX(outra.recebidaEm)
                  FROM LeituraSensor outra
                  WHERE outra.flange.id = l.flange.id
              )
              AND l.id = (
                  SELECT MAX(desempate.id)
                  FROM LeituraSensor desempate
                  WHERE desempate.flange.id = l.flange.id
                    AND desempate.recebidaEm = l.recebidaEm
              )
            """)
    List<LeituraSensor> findUltimasPorFlanges(@Param("flangeIds") List<Long> flangeIds);

    /**
     * Última leitura por sensor instalado: impede que uma leitura antiga de
     * outro sensor na mesma flange seja atribuída ao novo equipamento.
     * Empate de horário resolvido pelo maior identificador.
     */
    @EntityGraph(attributePaths = {"sensor", "flange"})
    @Query("""
            SELECT l FROM LeituraSensor l
            WHERE l.sensor.id IN :sensorIds
              AND l.recebidaEm = (
                  SELECT MAX(outra.recebidaEm)
                  FROM LeituraSensor outra
                  WHERE outra.sensor.id = l.sensor.id
              )
              AND l.id = (
                  SELECT MAX(desempate.id)
                  FROM LeituraSensor desempate
                  WHERE desempate.sensor.id = l.sensor.id
                    AND desempate.recebidaEm = l.recebidaEm
              )
            """)
    List<LeituraSensor> findUltimasPorSensores(@Param("sensorIds") List<Long> sensorIds);


}
