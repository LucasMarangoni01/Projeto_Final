package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class CentralAlarmesService {
    private final UsuarioRepository usuarios;
    private final FlangeRepository flanges;
    private final IncidenteRepository incidentes;
    private final TesteEmergenciaRepository testes;
    private final UsuarioAuditoriaService auditoria;

    public CentralAlarmesService(UsuarioRepository usuarios, FlangeRepository flanges,
            IncidenteRepository incidentes, TesteEmergenciaRepository testes,
            UsuarioAuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.flanges = flanges;
        this.incidentes = incidentes;
        this.testes = testes;
        this.auditoria = auditoria;
    }

    public record Painel(List<AlarmePainelDTO> ativos, List<AlarmePainelDTO> recentes,
                         List<AlarmePainelDTO> testesAtivos, List<AlarmePainelDTO> testesHistorico) {}

    @Transactional(readOnly = true)
    public Painel painel(Authentication auth) {
        Usuario u = usuario(auth);
        boolean admin = u.getRole() == Role.ADMIN;
        List<Incidente> ativos = admin
                ? incidentes.findByDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc()
                : incidentes.findByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(u.getUnidade().getId());
        List<Incidente> recentes = admin
                ? incidentes.findTop6ByDataHoraIsNotNullOrderByDataHoraDesc()
                : incidentes.findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(u.getUnidade().getId());
        return new Painel(ativos.stream().map(AlarmePainelDTO::real).toList(),
                recentes.stream().map(AlarmePainelDTO::real).toList(),
                admin ? testes.findByEstadoNotOrderByDetectadoEmDesc(StatusAlarme.NORMALIZADO)
                        .stream().map(AlarmePainelDTO::teste).toList() : List.of(),
                admin ? testes.findTop20ByEstadoOrderByDetectadoEmDesc(StatusAlarme.NORMALIZADO)
                        .stream().map(AlarmePainelDTO::teste).toList() : List.of());
    }

    @Transactional
    public AlarmePainelDTO iniciar(Authentication auth, Long flangeId, TesteEmergencia.Tipo tipo,
            TesteEmergencia.Gravidade gravidade, boolean confirmado) {
        Usuario u = admin(auth);
        if (!confirmado || flangeId == null || tipo == null || gravidade == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirme e preencha os dados do teste.");
        }
        Flange flange = flanges.findById(flangeId).filter(Flange::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flange ativa não encontrada."));
        TesteEmergencia t = testes.saveAndFlush(new TesteEmergencia(flange, tipo, gravidade, u.getEmail(), LocalDateTime.now()));
        auditar(u, t, "TESTE_EMERGENCIA_INICIADO");
        return AlarmePainelDTO.teste(t);
    }

    @Transactional
    public AlarmePainelDTO transicao(Authentication auth, Long id, String acao) {
        Usuario u = admin(auth);
        TesteEmergencia t = testes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDateTime agora = LocalDateTime.now();
        try {
            switch (acao) {
                case "reconhecer" -> t.reconhecer(u.getEmail(), agora);
                case "atender" -> t.atender(u.getEmail(), agora);
                case "encerrar" -> t.encerrar(u.getEmail(), agora);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        testes.saveAndFlush(t);
        auditar(u, t, "TESTE_EMERGENCIA_" + acao.toUpperCase(java.util.Locale.ROOT));
        return AlarmePainelDTO.teste(t);
    }

    @Transactional
    public void atenderReal(Authentication auth, Long id) {
        Usuario u = usuario(auth);
        Incidente i = incidentePermitido(u, id);
        if (!i.isAlarmeAtivo() || !CicloAlarme.podeAtender(i.getStatusAlarme()) ||
                incidentes.marcarEmAtendimento(id, StatusAlarme.RECONHECIDO, StatusAlarme.EM_MANUTENCAO, LocalDateTime.now(), u.getEmail()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reconheça a ocorrência ativa antes de iniciar atendimento.");
        }
        auditoria.registrar(u, u.getEmail(), "ALARME_EM_ATENDIMENTO", "Ocorrência real #" + id);
    }

    @Transactional
    public void reconhecerReal(Authentication auth, Long id, String justificativa) {
        Usuario u = usuario(auth);
        Incidente i = incidentePermitido(u, id);
        String motivo = justificativa == null ? "" : justificativa.trim();
        if (motivo.isBlank() || motivo.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma justificativa de até 500 caracteres.");
        }
        if (!i.isAlarmeAtivo() || !CicloAlarme.aguardaReconhecimento(i.getStatusAlarme()) ||
                incidentes.reconhecerAtivo(id, StatusAlarme.DISPARADO, StatusAlarme.RECONHECIDO,
                        LocalDateTime.now(), u.getEmail(), motivo) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A ocorrência já foi reconhecida ou encerrada.");
        }
        auditoria.registrar(u, u.getEmail(), "ALARME_RECONHECIDO", "Ocorrência real #" + id);
    }

    private Incidente incidentePermitido(Usuario u, Long id) {
        Incidente i = incidentes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (i.getFlange() == null || (u.getRole() != Role.ADMIN &&
                (i.getFlange().getUnidade() == null || !i.getFlange().getUnidade().isAtivo()
                  || !u.getUnidade().getId().equals(i.getFlange().getUnidade().getId())))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return i;
    }

    private void auditar(Usuario u, TesteEmergencia t, String acao) {
        auditoria.registrar(u, u.getEmail(), acao,
                "Teste #" + t.getId() + " | flange #" + t.getFlangeId() + " | " + t.getTipo() + " | " + t.getGravidade());
    }

    private Usuario usuario(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Usuario u = usuarios.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!u.isAtivo() || (u.getRole() != Role.ADMIN &&
                (u.getUnidade() == null || !u.getUnidade().isAtivo()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return u;
    }
    private Usuario admin(Authentication auth) {
        Usuario u = usuario(auth);
        if (u.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return u;
    }
}
