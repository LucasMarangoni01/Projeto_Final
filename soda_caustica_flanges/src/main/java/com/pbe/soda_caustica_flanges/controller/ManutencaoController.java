package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.model.Funcionario;
import com.pbe.soda_caustica_flanges.model.Manutencao;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.FuncionarioRepository;
import com.pbe.soda_caustica_flanges.repository.ManutencaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ManutencaoController {

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private FlangeRepository flangeRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @GetMapping("/manutencao/listagem")
    public String listagem(Model model) {
        model.addAttribute("manutencoes", manutencaoRepository.findAll());
        return "manutencao/listagem";
    }

    @GetMapping("/manutencao/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("manutencao", new Manutencao());
        return "manutencao/cadastro";
    }

    @PostMapping("/manutencao/salvar")
    public String salvar(@ModelAttribute("manutencao") Manutencao manutencao,
                         @RequestParam("flangeId") Long flangeId,
                         @RequestParam("funcionarioId") Long funcionarioId,
                         Model model) {

        Flange flange = flangeRepository.findById(flangeId).orElse(null);

        if (flange == null) {
            model.addAttribute("erro", "Flange não encontrada. Digite um ID de flange existente.");
            return "manutencao/cadastro";
        }

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId).orElse(null);

        if (funcionario == null) {
            model.addAttribute("erro", "Funcionário não encontrado. Digite um ID de funcionário existente.");
            return "manutencao/cadastro";
        }

        manutencao.setFlange(flange);
        manutencao.setFuncionario(funcionario);

        manutencaoRepository.save(manutencao);

        return "redirect:/manutencao/listagem";
    }

    @GetMapping("/manutencao/alterar/{id}")
    public String alterar(@PathVariable Long id, Model model) {

        Manutencao manutencao = manutencaoRepository.findById(id).orElse(null);

        if (manutencao == null) {
            return "redirect:/manutencao/listagem";
        }

        model.addAttribute("manutencao", manutencao);

        return "manutencao/cadastro";
    }

    @GetMapping("/manutencao/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        manutencaoRepository.deleteById(id);
        return "redirect:/manutencao/listagem";
    }
}