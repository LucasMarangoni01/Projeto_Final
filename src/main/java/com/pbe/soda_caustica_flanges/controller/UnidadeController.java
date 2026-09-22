package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute(
                "unidades",
                unidadeService.listarTodas()
        );

        return "unidade/listagem";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute(
                "unidade",
                new Unidade()
        );

        return "unidade/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("unidade") Unidade unidade,
            BindingResult result) {

        if (result.hasErrors()) {
            return "unidade/cadastro";
        }

        unidadeService.salvar(unidade);

        return "redirect:/unidades";
    }

    @PostMapping("/alterar-status")
    public String alterarStatus(
            @RequestParam Long id) {

        unidadeService.alterarStatus(id);

        return "redirect:/unidades";
    }
}