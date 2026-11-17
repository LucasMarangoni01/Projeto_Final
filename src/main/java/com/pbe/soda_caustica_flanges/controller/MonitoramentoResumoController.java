package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Resumo compacto do monitoramento para atualização periódica das páginas.
 * Nunca retorna dados de outras unidades para usuários USER.
 * A ausência de leituras não equivale a ausência de risco.
 */
@RestController
@RequestMapping("/api/monitoramento")
public class MonitoramentoResumoController {

    private final UsuarioRepository usuarioRepository;
    private final IncidenteRepository incidenteRepository;
    private final LeituraSensorRepository leituraRepository;

    public MonitoramentoResumoController(
            UsuarioRepository usuarioRepository,
            IncidenteRepository incidenteRepository,
            LeituraSensorRepository leituraRepository) {
        this.usuarioRepository = usuarioRepository;
        this.incidenteRepository = incidenteRepository;
        this.leituraRepository = leituraRepository;
    }

    @GetMapping("/resumo")
    @Transactional(readOnly = true)
    public ResponseEntity<Resumo> resumo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        long ativos;
        LocalDateTime ultimaLeitura;
        if (usuario.getRole() == Role.ADMIN) {
            ativos = incidenteRepository.countByDataHoraIsNotNullAndNormalizadoEmIsNull();
            ultimaLeitura = leituraRepository.findTopByOrderByRecebidaEmDesc()
                    .map(LeituraSensor::getRecebidaEm).orElse(null);
        } else {
            Unidade unidade = usuario.getUnidade();
            if (unidade == null || !unidade.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            ativos = incidenteRepository
                    .countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(
                            unidade.getId());
            ultimaLeitura = leituraRepository
                    .findTopByFlange_Unidade_IdOrderByRecebidaEmDesc(unidade.getId())
                    .map(LeituraSensor::getRecebidaEm).orElse(null);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new Resumo(ativos, ultimaLeitura == null ? null : ultimaLeitura.toString()));
    }

    public record Resumo(long alertasAtivos, String ultimaLeituraEm) {
    }
}
