package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.IncidenteRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Alimenta o painel de monitoramento com os 6 alertas reais mais recentes.
 * O endpoint não permite escrever ou criar alertas simulados no banco.
 */
@RestController
@RequestMapping("/api/monitoramento")
public class MonitoramentoEventosController {

    private final UsuarioRepository usuarios;
    private final IncidenteRepository incidentes;

    public MonitoramentoEventosController(
            UsuarioRepository usuarios, IncidenteRepository incidentes) {
        this.usuarios = usuarios;
        this.incidentes = incidentes;
    }

    @GetMapping("/eventos")
    @Transactional(readOnly = true)
    public ResponseEntity<Painel> listar(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarios.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        long totalAtivos;
        List<Incidente> recentes;
        List<Long> flangesEmAlerta;
        if (usuario.getRole() == Role.ADMIN) {
            totalAtivos = incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull();
            recentes = incidentes.findTop6ByDataHoraIsNotNullOrderByDataHoraDesc();
            flangesEmAlerta = incidentes.findIdsFlangesComAlertasAtivos();
        } else {
            Unidade unidade = usuario.getUnidade();
            if (unidade == null || !unidade.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            totalAtivos = incidentes.countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(unidade.getId());
            recentes = incidentes.findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
                    unidade.getId());
            flangesEmAlerta = incidentes.findIdsFlangesComAlertasAtivosPorUnidade(unidade.getId());
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new Painel(
                        recentes.stream().map(EventoMonitoramentoDTO::de).toList(),
                        flangesEmAlerta,
                        totalAtivos
                ));
    }

    public record Painel(List<EventoMonitoramentoDTO> eventos, List<Long> idsFlangesEmAlerta, long totalAlertasAtivos) {
    }
}
