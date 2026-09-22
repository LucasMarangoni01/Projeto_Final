package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.StatusFlange;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.UnidadeRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class FlangeController {

    @Autowired
    private FlangeRepository flangeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    private final String uploadDir =
            "src/main/resources/static/foto_flange/";

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

            if (usuario.getUnidade() == null) {

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

            if (arquivoFoto != null &&
                    !arquivoFoto.isEmpty()) {

                String nomeArquivo =
                        UUID.randomUUID()
                                + "_"
                                + arquivoFoto.getOriginalFilename();

                Path diretorio =
                        Paths.get(uploadDir)
                                .toAbsolutePath();

                if (!Files.exists(diretorio)) {
                    Files.createDirectories(diretorio);
                }

                Path arquivo =
                        diretorio.resolve(nomeArquivo);

                arquivoFoto.transferTo(
                        arquivo.toFile()
                );

                flange.setFoto(nomeArquivo);

            } else if (flange.getId() != null) {

                Flange flangeBanco =
                        flangeRepository
                                .findById(flange.getId())
                                .orElse(null);

                if (flangeBanco != null) {

                    flange.setFoto(
                            flangeBanco.getFoto()
                    );

                    if (flangeBanco.getStatus() != null) {

                        flange.setStatus(
                                flangeBanco.getStatus()
                        );
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (flange.getStatus() == null) {
            flange.setStatus(
                    StatusFlange.NORMAL
            );
        }

        flangeRepository.save(flange);

        return "redirect:/flange/listagem";
    }

    @GetMapping("/flange/excluir/{id}")
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

        flangeRepository.delete(flange);

        return "redirect:/flange/listagem";
    }
}