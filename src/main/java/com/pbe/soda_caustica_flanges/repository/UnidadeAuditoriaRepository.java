package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.UnidadeAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeAuditoriaRepository extends JpaRepository<UnidadeAuditoria, Long> {

    List<UnidadeAuditoria> findTop30ByUnidadeIdOrderByDataEventoDesc(Long unidadeId);
    java.util.Optional<UnidadeAuditoria> findFirstByUnidadeIdAndAcaoOrderByDataEventoAsc(
            Long unidadeId,
            String acao
    );
}
