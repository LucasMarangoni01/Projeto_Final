package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Funcionario;
import com.pbe.soda_caustica_flanges.repository.FuncionarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class FuncionarioController {

    @Autowired
    private FuncionarioRepository funcionarioRepository;


    // ==============================
    // LISTAGEM ( /funcionario/listagem )
    // ==============================
    @GetMapping("/funcionario/listagem")
    public String listagem(Model model) {
        model.addAttribute("funcionarios", funcionarioRepository.findAll());
        // Adicionado o caminho da pasta
        return "funcionario/listagem";
    }

    // ==============================
    // CADASTRO ( /funcionario/cadastro )
    // ==============================
    @GetMapping("/funcionario/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("funcionario", new Funcionario());
        // Adicionado o caminho da pasta
        return "funcionario/cadastro";
    }

    // ==============================
    // SALVAR (POST /funcionario/salvar)
    // ==============================
    @PostMapping("/funcionario/salvar")
    public String salvar(@Valid Funcionario funcionario, BindingResult result) {

        if (result.hasErrors()) {
            // Adicionado o caminho da pasta em caso de erro na validação
            return "funcionario/listagem";
        }

        funcionarioRepository.save(funcionario);
        return "redirect:/funcionario/listagem";
    }

    // ==============================
    // ALTERAR ( /funcionario/alterar/{id} )
    // ==============================
    @GetMapping("/funcionario/alterar/{id}")
    public String alterar(@PathVariable Long id, Model model) {
        Funcionario funcionario = funcionarioRepository.findById(id).get();
        model.addAttribute("funcionario", funcionario);
        return "funcionario/alterar";
    }

    // ==============================
    // EXCLUIR ( /funcionario/excluir/{id} )
    // ==============================
    @GetMapping("/funcionario/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        funcionarioRepository.deleteById(id);
        return "redirect:/funcionario/listagem";
    }
}