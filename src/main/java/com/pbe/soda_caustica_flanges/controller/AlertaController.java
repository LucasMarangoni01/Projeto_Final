package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.IncidenteRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/alertas")
public class AlertaController {

    private final IncidenteRepository incidenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final FlangeRepository flangeRepository;

    public AlertaController(
            IncidenteRepository incidenteRepository,
            UsuarioRepository usuarioRepository,
            FlangeRepository flangeRepository) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.flangeRepository = flangeRepository;
    }

    @GetMapping
    public String listar(
            Authentication authentication,
            Model model,
            @RequestParam(value = "sucesso", required = false) String sucesso,
            @RequestParam(value = "erro", required = false) String erro) {

        Usuario usuario = buscarUsuario(authentication);
        List<Incidente> registros;

        if (usuario.getRole() == Role.ADMIN) {
            registros = incidenteRepository.findByDataHoraIsNotNullOrderByDataHoraDesc();
        } else if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            registros = Collections.emptyList();
        } else {
            registros = incidenteRepository.findByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
                    usuario.getUnidade().getId()
            );
        }

        List<Incidente> ativos = registros.stream()
                .filter(Incidente::isAlarmeAtivo)
                .toList();

        List<Incidente> historico = registros.stream()
                .filter(incidente -> incidente.getNormalizadoEm() != null)
                .toList();

        long disparados = ativos.stream()
                .filter(incidente -> incidente.getStatusAlarme() == StatusAlarme.DISPARADO)
                .count();

        long reconhecidos = ativos.stream()
                .filter(incidente -> incidente.getStatusAlarme() == StatusAlarme.RECONHECIDO
                        || incidente.getStatusAlarme() == StatusAlarme.EM_MANUTENCAO)
                .count();

        model.addAttribute("alertasAtivos", ativos);
        model.addAttribute("historicoAlertas", historico);
        model.addAttribute("totalAtivos", ativos.size());
        model.addAttribute("totalDisparados", disparados);
        model.addAttribute("totalReconhecidos", reconhecidos);
        model.addAttribute("ehAdmin", usuario.getRole() == Role.ADMIN);
        model.addAttribute("sucesso", mensagemSucesso(sucesso));
        model.addAttribute("erro", mensagemErro(erro));

        return "alerta/listagem";
    }

    @PostMapping("/{id}/reconhecer")
    public String reconhecer(
            @PathVariable Long id,
            @RequestParam("justificativa") String justificativa,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = buscarUsuario(authentication);
        Incidente incidente = buscarAlerta(id);

        if (!podeConsultar(incidente, usuario)) {
            return "redirect:/alertas?erro=permissao";
        }

        if (!incidente.isAlarmeAtivo()) {
            return "redirect:/alertas?erro=encerrado";
        }

        if (incidente.getStatusAlarme() == StatusAlarme.RECONHECIDO
                || incidente.getStatusAlarme() == StatusAlarme.EM_MANUTENCAO) {
            return "redirect:/alertas?erro=ja_reconhecido";
        }

        String motivo = validarJustificativa(justificativa);
        if (motivo == null) {
            return "redirect:/alertas?erro=justificativa";
        }

        incidente.setStatusAlarme(StatusAlarme.RECONHECIDO);
        incidente.setReconhecidoEm(LocalDateTime.now());
        incidente.setReconhecidoPorEmail(usuario.getEmail());
        incidente.setJustificativaReconhecimento(motivo);
        incidenteRepository.save(incidente);

        redirectAttributes.addAttribute("sucesso", "reconhecido");
        return "redirect:/alertas";
    }

    @PostMapping("/{id}/normalizar")
    public String normalizar(
            @PathVariable Long id,
            @RequestParam("justificativa") String justificativa,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = buscarUsuario(authentication);
        if (usuario.getRole() != Role.ADMIN) {
            return "redirect:/alertas?erro=permissao";
        }

        Incidente incidente = buscarAlerta(id);
        if (incidente == null || !incidente.isAlarmeAtivo()) {
            return "redirect:/alertas?erro=encerrado";
        }

        if (incidente.getStatusAlarme() != StatusAlarme.RECONHECIDO
                && incidente.getStatusAlarme() != StatusAlarme.EM_MANUTENCAO) {
            return "redirect:/alertas?erro=reconhecimento_obrigatorio";
        }

        String motivo = validarJustificativa(justificativa);
        if (motivo == null) {
            return "redirect:/alertas?erro=justificativa";
        }

        incidente.setStatusAlarme(StatusAlarme.NORMALIZADO);
        incidente.setNormalizadoEm(LocalDateTime.now());
        incidente.setNormalizadoPorEmail(usuario.getEmail());
        incidente.setJustificativaNormalizacao(motivo);
        incidenteRepository.saveAndFlush(incidente);

        Flange flange = incidente.getFlange();
        if (flange != null && flange.isAtivo()) {
            boolean existeOutroAlarme = incidenteRepository
                    .existsByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullAndIdNot(
                            flange.getId(),
                            incidente.getId()
                    );

            if (!existeOutroAlarme) {
                StatusFlange statusAnterior = incidente.getStatusFlangeAnterior();
                flange.setStatus(statusAnterior == null ? StatusFlange.NORMAL : statusAnterior);
                flangeRepository.save(flange);
            }
        }

        redirectAttributes.addAttribute("sucesso", "normalizado");
        return "redirect:/alertas";
    }

    private Usuario buscarUsuario(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private Incidente buscarAlerta(Long id) {
        return incidenteRepository.findById(id)
                .filter(incidente -> incidente.getDataHora() != null)
                .orElse(null);
    }

    private boolean podeConsultar(Incidente incidente, Usuario usuario) {
        if (incidente == null || incidente.getFlange() == null
                || incidente.getFlange().getUnidade() == null) {
            return false;
        }

        if (usuario.getRole() == Role.ADMIN) {
            return true;
        }

        return usuario.getUnidade() != null
                && usuario.getUnidade().isAtivo()
                && incidente.getFlange().getUnidade().isAtivo()
                && incidente.getFlange().getUnidade().getId()
                        .equals(usuario.getUnidade().getId());
    }

    private String validarJustificativa(String justificativa) {
        if (justificativa == null) {
            return null;
        }

        String motivo = justificativa.trim();
        return motivo.isBlank() || motivo.length() > 500 ? null : motivo;
    }

    private String mensagemSucesso(String sucesso) {
        if ("reconhecido".equals(sucesso)) {
            return "Alerta reconhecido. O registro continua ativo e permanece no histórico.";
        }
        if ("normalizado".equals(sucesso)) {
            return "Alerta normalizado. A ocorrência foi preservada no histórico.";
        }
        return null;
    }

    private String mensagemErro(String erro) {
        if ("permissao".equals(erro)) {
            return "Você não tem permissão para executar esta ação neste alerta.";
        }
        if ("encerrado".equals(erro)) {
            return "Este alerta já foi normalizado ou não está disponível.";
        }
        if ("ja_reconhecido".equals(erro)) {
            return "Este alerta já foi reconhecido.";
        }
        if ("justificativa".equals(erro)) {
            return "Informe uma justificativa válida, com até 500 caracteres.";
        }
        if ("reconhecimento_obrigatorio".equals(erro)) {
            return "Reconheça o alerta antes de registrar sua normalização.";
        }
        return null;
    }
}
