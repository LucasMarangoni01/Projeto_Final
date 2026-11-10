package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Funcionario;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.FuncionarioRepository;
import com.pbe.soda_caustica_flanges.repository.ManutencaoRepository;
import com.pbe.soda_caustica_flanges.repository.UnidadeRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
public class FuncionarioController {

    private final FuncionarioRepository funcionarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ManutencaoRepository manutencaoRepository;

    public FuncionarioController(
            FuncionarioRepository funcionarioRepository,
            UnidadeRepository unidadeRepository,
            UsuarioRepository usuarioRepository,
            ManutencaoRepository manutencaoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.manutencaoRepository = manutencaoRepository;
    }

    @GetMapping("/funcionario/listagem")
    public String listagem(Model model, Authentication authentication) {
        Usuario usuario = buscarUsuarioLogado(authentication);
        boolean admin = usuario.getRole() == Role.ADMIN;

        List<Funcionario> funcionarios;
        if (admin) {
            funcionarios = funcionarioRepository.findAll();
        } else if (usuario.getUnidade() == null) {
            funcionarios = Collections.emptyList();
        } else {
            funcionarios = funcionarioRepository.findByUnidadeIdOrderByIdAsc(
                    usuario.getUnidade().getId()
            );
        }

        model.addAttribute("funcionarios", funcionarios);
        model.addAttribute("ehAdmin", admin);
        return "funcionario/listagem";
    }

    @GetMapping("/funcionario/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("funcionario", new Funcionario());
        model.addAttribute("unidades", listarUnidadesAtivas());
        return "funcionario/cadastro";
    }

    @PostMapping("/funcionario/salvar")
    public String salvar(
            @Valid @ModelAttribute("funcionario") Funcionario funcionario,
            BindingResult result,
            @RequestParam(value = "unidadeId", required = false) Long unidadeId,
            Model model) {

        if (unidadeId == null) {
            result.reject("unidade.obrigatoria", "Selecione a unidade do funcionário.");
        }

        Unidade unidade = unidadeId == null
                ? null
                : unidadeRepository.findById(unidadeId).orElse(null);

        if (unidade == null || !unidade.isAtivo()) {
            result.reject("unidade.invalida", "Selecione uma unidade ativa.");
        }

        if (result.hasErrors()) {
            model.addAttribute("unidades", listarUnidadesAtivas());
            return funcionario.getId() == null
                    ? "funcionario/cadastro"
                    : "funcionario/alterar";
        }

        Funcionario existente = funcionario.getId() == null
                ? null
                : funcionarioRepository.findById(funcionario.getId()).orElse(null);

        if (funcionario.getId() != null && existente == null) {
            return "redirect:/funcionario/listagem?erro=nao-encontrado";
        }

        if (existente != null && existente.getUsuario() != null) {
            Usuario conta = existente.getUsuario();
            conta.setNome(funcionario.getNome_funcionario().trim());
            conta.setUnidade(unidade);
            usuarioRepository.save(conta);
            funcionario.setUsuario(conta);
        }

        funcionario.setUnidade(unidade);
        funcionarioRepository.save(funcionario);
        return "redirect:/funcionario/listagem";
    }

    @GetMapping("/funcionario/alterar/{id}")
    public String alterar(@PathVariable Long id, Model model) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));
        model.addAttribute("funcionario", funcionario);
        model.addAttribute("unidades", listarUnidadesAtivas());
        return "funcionario/alterar";
    }

    @PostMapping("/funcionario/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id).orElse(null);
        if (funcionario == null) {
            return "redirect:/funcionario/listagem";
        }

        if (funcionario.getUsuario() != null) {
            return "redirect:/funcionario/listagem?erro=conta";
        }

        if (manutencaoRepository.countByFuncionario_Id(id) > 0) {
            return "redirect:/funcionario/listagem?erro=historico";
        }

        funcionarioRepository.deleteById(id);
        return "redirect:/funcionario/listagem?sucesso=excluido";
    }

    private Usuario buscarUsuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private List<Unidade> listarUnidadesAtivas() {
        return unidadeRepository.findAll()
                .stream()
                .filter(Unidade::isAtivo)
                .toList();
    }
}
