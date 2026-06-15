package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;

@Controller
public class FlangeController {

    @Autowired
    private FlangeRepository flangeRepository;

    private final String uploadDir = "src/main/resources/static/foto_flange/";

    // ==============================
    // LISTAGEM
    // ==============================
    @GetMapping("/flange/listagem")
    public String listagem(Model model) {

        model.addAttribute("flanges", flangeRepository.findAll());

        return "/flange/listagem";
    }

    // ==============================
    // CADASTRO
    // ==============================
    @GetMapping("/flange/cadastro")
    public String cadastro(Model model) {

        model.addAttribute("flange", new Flange());

        return "/flange/cadastro";
    }

    // ==============================
    // ALTERAR
    // ==============================
    @GetMapping("/flange/alterar/{id}")
    public String alterar(@PathVariable Long id, Model model){

        Flange flange = flangeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flange inválida: " + id));

        model.addAttribute("flange", flange);

        return "/flange/alterar";
    }

    // ==============================
    // SALVAR
    // ==============================
    @PostMapping("/flange/salvar")
    public String salvar(@Valid Flange flange,
                         BindingResult result,
                         @RequestParam(value="arquivoFoto", required=false) MultipartFile arquivoFoto) {

        if (result.hasErrors()) {

            if(flange.getId() != null){
                return "/flange/alterar";
            }

            return "/flange/cadastro";
        }

        try {

            if (arquivoFoto != null && !arquivoFoto.isEmpty()) {

                String nomeArquivo = UUID.randomUUID() + "_" + arquivoFoto.getOriginalFilename();

                Path caminhoDiretorio = Paths.get(uploadDir).toAbsolutePath();

                if (!Files.exists(caminhoDiretorio)) {
                    Files.createDirectories(caminhoDiretorio);
                }

                Path caminhoArquivo = caminhoDiretorio.resolve(nomeArquivo);

                arquivoFoto.transferTo(caminhoArquivo.toFile());

                flange.setFoto(nomeArquivo);
            }
            else if (flange.getId() != null) {

                Flange flangeBanco = flangeRepository.findById(flange.getId()).orElse(null);

                if (flangeBanco != null) {
                    flange.setFoto(flangeBanco.getFoto());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        flangeRepository.save(flange);

        return "redirect:/flange/listagem";
    }


    // ==============================
    // EXCLUIR
    // ==============================
    @GetMapping("/flange/excluir/{id}")
    public String excluir(@PathVariable Long id) {

        flangeRepository.deleteById(id);

        return "redirect:/flange/listagem";
    }
}