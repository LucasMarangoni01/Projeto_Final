package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.IncidenteRepository;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import com.pbe.soda_caustica_flanges.repository.LeituraSensorRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final FlangeRepository flanges;
    private final SensorRepository sensores;
    private final LeituraSensorRepository leituras;

    public MonitoramentoEventosController(
            UsuarioRepository usuarios, IncidenteRepository incidentes,
            FlangeRepository flanges, SensorRepository sensores,
            LeituraSensorRepository leituras) {
        this.usuarios = usuarios;
        this.incidentes = incidentes;
        this.flanges = flanges;
        this.sensores = sensores;
        this.leituras = leituras;
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
        List<Incidente> ativos;
        List<Flange> permitidas;
        List<Long> flangesEmAlerta;
        if (usuario.getRole() == Role.ADMIN) {
            totalAtivos = incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull();
            recentes = incidentes.findTop6ByDataHoraIsNotNullOrderByDataHoraDesc();
            ativos = incidentes.findByDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc();
            permitidas = flanges.findAtivas();
            flangesEmAlerta = incidentes.findIdsFlangesComAlertasAtivos();
        } else {
            Unidade unidade = usuario.getUnidade();
            if (unidade == null || !unidade.isAtivo()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            totalAtivos = incidentes.countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(unidade.getId());
            recentes = incidentes.findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
                    unidade.getId());
            ativos = incidentes.findByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(
                    unidade.getId());
            permitidas = flanges.findAtivasPorUnidade(unidade.getId());
            flangesEmAlerta = incidentes.findIdsFlangesComAlertasAtivosPorUnidade(unidade.getId());
        }
        // Estado visual compartilhado com o primeiro carregamento do Dashboard.
        // Nenhum dispositivo é considerado conectado por estar só cadastrado.
        List<Long> ids = permitidas.stream().map(Flange::getId).toList();
        List<Sensor> sensoresPermitidos = ids.isEmpty()
                ? List.of() : sensores.findByFlange_IdIn(ids);
        List<LeituraSensor> ultimas = ids.isEmpty()
                ? List.of() : leituras.findUltimasPorFlanges(ids);
        Set<Long> comLeitura = ultimas.stream()
                .filter(l -> l.getFlange() != null)
                .map(l -> l.getFlange().getId())
                .collect(Collectors.toSet());
        Set<Long> comSensorInstalado = sensoresPermitidos.stream()
                .filter(s -> s.getStatus() == StatusSensor.INSTALADO && s.getFlange() != null)
                .map(s -> s.getFlange().getId())
                .collect(Collectors.toSet());
        Map<Long, List<Incidente>> ativosPorFlange = ativos.stream()
                .filter(i -> i.getFlange() != null && i.getFlange().getId() != null)
                .collect(Collectors.groupingBy(i -> i.getFlange().getId()));
        List<EstadoVisualFlangeDTO> estados = permitidas.stream()
                .map(flange -> EstadoVisualFlangeDTO.classificar(
                        flange,
                        ativosPorFlange.getOrDefault(flange.getId(), List.of()),
                        comSensorInstalado.contains(flange.getId()),
                        comLeitura.contains(flange.getId())))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new Painel(
                        recentes.stream().map(EventoMonitoramentoDTO::de).toList(),
                        flangesEmAlerta,
                        totalAtivos,
                        estados
                ));
    }

    public record Painel(List<EventoMonitoramentoDTO> eventos, List<Long> idsFlangesEmAlerta, long totalAlertasAtivos,
                         List<EstadoVisualFlangeDTO> estadosFlanges) {
    }
}
