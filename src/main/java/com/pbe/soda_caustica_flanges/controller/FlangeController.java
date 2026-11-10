package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.StatusFlange;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.ManutencaoRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import com.pbe.soda_caustica_flanges.repository.UnidadeRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import com.pbe.soda_caustica_flanges.service.FotoFlangeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class FlangeController {

    private final FlangeRepository flangeRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final SensorRepository sensorRepository;
    private final FotoFlangeService fotoFlangeService;

    public FlangeController(
            FlangeRepository flangeRepository,
            UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository,
            ManutencaoRepository manutencaoRepository,
            SensorRepository sensorRepository,
            FotoFlangeService fotoFlangeService) {
        this.flangeRepository = flangeRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.sensorRepository = sensorRepository;
        this.fotoFlangeService = fotoFlangeService;
    }

    /**
     * Entrega a imagem somente a usuários autenticados e autorizados
     * a visualizar a flange da própria unidade.
     */
    @GetMapping("/flange/foto/{id}")
    public ResponseEntity<byte[]> foto(
            @PathVariable Long id,
            Authentication authentication) {

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElse(null);
        Flange flange = flangeRepository.findById(id).orElse(null);

        if (usuario == null || flange == null || flange.getFoto() == null
                || flange.getFoto().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        if (usuario.getRole() != Role.ADMIN) {
            if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()
                    || flange.getUnidade() == null
                    || !flange.getUnidade().getId().equals(usuario.getUnidade().getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            FotoFlangeService.FotoCarregada foto = fotoFlangeService.carregar(flange.getFoto());
            return ResponseEntity.ok()
                    .contentType(foto.contentType())
                    .cacheControl(CacheControl.noStore())
                    .body(foto.bytes());
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/flange/listagem")
    public String listagem(
            Model model,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(
                        authentication.getName()
                ).orElseThrow();

        if (usuario.getRole() == Role.ADMIN) {

            model.addAttribute(
                    "flanges",
                    flangeRepository.findAll()
            );

        } else {

            if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {

                model.addAttribute(
                        "flanges",
                        java.util.Collections.emptyList()
                );

            } else {

                model.addAttribute(
                        "flanges",
                        flangeRepository.findByUnidadeId(
                                usuario.getUnidade().getId()
                        )
                );
            }
        }

        return "flange/listagem";
    }

    @GetMapping("/flange/cadastro")
    public String cadastro(
            Model model,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(
                        authentication.getName()
                ).orElseThrow();

        Flange flange = new Flange();

        flange.setStatus(StatusFlange.NORMAL);

        model.addAttribute(
                "flange",
                flange
        );

        model.addAttribute(
                "ehAdmin",
                usuario.getRole() == Role.ADMIN
        );

        if (usuario.getRole() == Role.ADMIN) {

            model.addAttribute(
                    "unidades",
                    unidadeRepository.findAll()
                            .stream()
                            .filter(Unidade::isAtivo)
                            .toList()
            );
        }

        return "flange/cadastro";
    }

    @GetMapping("/flange/alterar/{id}")
    public String alterar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(
                        authentication.getName()
                ).orElseThrow();

        Flange flange =
                flangeRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Flange inválida: " + id
                                )
                        );

        if (usuario.getRole() != Role.ADMIN) {

            if (usuario.getUnidade() == null ||
                    flange.getUnidade() == null ||
                    !flange.getUnidade()
                            .getId()
                            .equals(usuario.getUnidade().getId())) {

                return "redirect:/flange/listagem";
            }
        }

        model.addAttribute(
                "flange",
                flange
        );

        model.addAttribute(
                "ehAdmin",
                usuario.getRole() == Role.ADMIN
        );

        if (usuario.getRole() == Role.ADMIN) {

            model.addAttribute(
                    "unidades",
                    unidadeRepository.findAll()
                            .stream()
                            .filter(Unidade::isAtivo)
                            .toList()
            );
        }

        return "flange/alterar";
    }

    @PostMapping("/flange/salvar")
    public String salvar(
            @Valid Flange flange,
            BindingResult result,
            @RequestParam(
                    value = "arquivoFoto",
                    required = false
            ) MultipartFile arquivoFoto,
            @RequestParam(
                    value = "unidadeId",
                    required = false
            ) Long unidadeId,
            Authentication authentication,
            Model model) {

        Usuario usuario =
                usuarioRepository.findByEmail(
                        authentication.getName()
                ).orElseThrow();

        if (result.hasErrors()) {

            model.addAttribute(
                    "ehAdmin",
                    usuario.getRole() == Role.ADMIN
            );

            if (usuario.getRole() == Role.ADMIN) {

                model.addAttribute(
                        "unidades",
                        unidadeRepository.findAll()
                                .stream()
                                .filter(Unidade::isAtivo)
                                .toList()
                );
            }

            if (flange.getId() != null) {
                return "flange/alterar";
            }

            return "flange/cadastro";
        }

        Flange flangeExistente = null;

        if (flange.getId() != null) {
            flangeExistente = flangeRepository.findById(flange.getId()).orElse(null);

            if (flangeExistente == null) {
                return "redirect:/flange/listagem";
            }

            if (usuario.getRole() != Role.ADMIN) {
                if (usuario.getUnidade() == null
                        || flangeExistente.getUnidade() == null
                        || !flangeExistente.getUnidade().getId()
                                .equals(usuario.getUnidade().getId())) {
                    return "redirect:/flange/listagem";
                }
            }
        }

        if (usuario.getRole() == Role.ADMIN) {

            if (unidadeId == null) {

                model.addAttribute(
                        "erro",
                        "Selecione uma unidade."
                );

                model.addAttribute(
                        "ehAdmin",
                        true
                );

                model.addAttribute(
                        "unidades",
                        unidadeRepository.findAll()
                                .stream()
                                .filter(Unidade::isAtivo)
                                .toList()
                );

                if (flange.getId() != null) {
                    return "flange/alterar";
                }

                return "flange/cadastro";
            }

            Unidade unidade =
                    unidadeRepository.findById(
                            unidadeId
                    ).orElse(null);

            if (unidade == null ||
                    !unidade.isAtivo()) {

                model.addAttribute(
                        "erro",
                        "Unidade inválida."
                );

                model.addAttribute(
                        "ehAdmin",
                        true
                );

                model.addAttribute(
                        "unidades",
                        unidadeRepository.findAll()
                                .stream()
                                .filter(Unidade::isAtivo)
                                .toList()
                );

                if (flange.getId() != null) {
                    return "flange/alterar";
                }

                return "flange/cadastro";
            }

            flange.setUnidade(unidade);

        } else {

            if (usuario.getUnidade() == null) {

                model.addAttribute(
                        "erro",
                        "Sua conta ainda não possui uma unidade."
                );

                return "flange/cadastro";
            }

            flange.setUnidade(
                    usuario.getUnidade()
            );
        }

        try {

            if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
                flange.setFoto(fotoFlangeService.salvar(arquivoFoto));
            } else if (flangeExistente != null) {
                flange.setFoto(flangeExistente.getFoto());
            }

            if (flangeExistente != null) {
                flange.setStatus(flangeExistente.getStatus());
            }

        } catch (IOException | IllegalArgumentException e) {
            model.addAttribute("erro",
                    e.getMessage() == null
                            ? "Não foi possível salvar a foto da flange."
                            : e.getMessage());
            model.addAttribute("ehAdmin", usuario.getRole() == Role.ADMIN);

            if (usuario.getRole() == Role.ADMIN) {
                model.addAttribute(
                        "unidades",
                        unidadeRepository.findAll().stream()
                                .filter(Unidade::isAtivo)
                                .toList()
                );
            }

            return flange.getId() != null
                    ? "flange/alterar"
                    : "flange/cadastro";
        }

        if (flange.getStatus() == null) {
            flange.setStatus(
                    StatusFlange.NORMAL
            );
        }

        flangeRepository.save(flange);

        return "redirect:/flange/listagem";
    }

    @PostMapping("/flange/excluir/{id}")
    public String excluir(
            @PathVariable Long id,
            Authentication authentication) {

        Usuario usuario =
                usuarioRepository.findByEmail(
                        authentication.getName()
                ).orElseThrow();

        Flange flange =
                flangeRepository.findById(id)
                        .orElse(null);

        if (flange == null) {
            return "redirect:/flange/listagem";
        }

        if (usuario.getRole() != Role.ADMIN) {

            if (usuario.getUnidade() == null ||
                    flange.getUnidade() == null ||
                    !flange.getUnidade()
                            .getId()
                            .equals(usuario.getUnidade().getId())) {

                return "redirect:/flange/listagem";
            }
        }

        if (manutencaoRepository.countByFlange_Id(id) > 0) {
            return "redirect:/flange/listagem?erro=historico";
        }

        if (sensorRepository.countByFlange_Id(id) > 0) {
            return "redirect:/flange/listagem?erro=sensores";
        }

        flangeRepository.delete(flange);

        return "redirect:/flange/listagem?sucesso=excluida";
    }
}