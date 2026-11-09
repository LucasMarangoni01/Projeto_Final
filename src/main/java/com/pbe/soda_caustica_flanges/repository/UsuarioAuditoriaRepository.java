package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.UsuarioAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioAuditoriaRepository extends JpaRepository<UsuarioAuditoria, Long> {

    List<UsuarioAuditoria> findTop30ByUsuarioIdOrderByDataEventoDesc(Long usuarioId);
}
