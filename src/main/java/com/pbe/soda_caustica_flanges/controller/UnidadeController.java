package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
    public String listar(Model model,
                         @RequestParam(required = false) String sucesso,
                         @RequestParam(required = false) String erro) {
        model.addAttribute("unidadesResumo", unidadeService.listarResumos());
        model.addAttribute("sucesso", sucesso);
        model.addAttribute("erro", erro);
        return "unidade/listagem";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("unidade", new Unidade());
        model.addAttribute("modoEdicao", false);
        return "unidade/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("unidade") Unidade unidade,
            BindingResult result,
            Authentication authentication,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("modoEdicao", unidade.getId() != null);
            return unidade.getId() == null ? "unidade/cadastro" : carregarDetalheComErros(unidade, model);
        }

        try {
            Unidade salva = unidadeService.salvar(unidade, authentication.getName());
            return "redirect:/unidades/editar/" + salva.getId() + "?sucesso=salva";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erroSalvar", e.getMessage());
            model.addAttribute("modoEdicao", unidade.getId() != null);
            return unidade.getId() == null ? "unidade/cadastro" : carregarDetalheComErros(unidade, model);
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            @RequestParam(required = false) String sucesso,
            @RequestParam(required = false) String erro,
            Model model) {

        Unidade unidade = unidadeService.buscarPorId(id);
        prepararDetalhe(model, unidade);
        model.addAttribute("sucesso", sucesso);
        model.addAttribute("erro", erro);
        return "unidade/editar";
    }

    @PostMapping("/adicionar-pessoa")
    public String adicionarPessoa(
            @RequestParam Long unidadeId,
            @RequestParam String identificador,
            Authentication authentication) {
        try {
            unidadeService.adicionarUsuario(
                    unidadeId,
                    identificador,
                    authentication.getName()
            );
            return "redirect:/unidades/editar/" + unidadeId + "?sucesso=pessoa-adicionada";
        } catch (IllegalArgumentException e) {
            return "redirect:/unidades/editar/" + unidadeId + "?erro=pessoa";
        }
    }

    @PostMapping("/remover-pessoa")
    public String removerPessoa(
            @RequestParam Long unidadeId,
            @RequestParam Long usuarioId,
            Authentication authentication) {
        try {
            unidadeService.removerUsuario(
                    unidadeId,
                    usuarioId,
                    authentication.getName()
            );
            return "redirect:/unidades/editar/" + unidadeId + "?sucesso=pessoa-removida";
        } catch (IllegalArgumentException e) {
            return "redirect:/unidades/editar/" + unidadeId + "?erro=pessoa";
        }
    }

    @PostMapping("/alterar-status")
    public String alterarStatus(
            @RequestParam Long id,
            Authentication authentication) {
        unidadeService.alterarStatus(id, authentication.getName());
        return "redirect:/unidades?sucesso=status";
    }

    @PostMapping("/excluir")
    public String excluir(
            @RequestParam Long id,
            Authentication authentication) {
        try {
            unidadeService.excluirUnidade(id, authentication.getName());
            return "redirect:/unidades?sucesso=excluida";
        } catch (IllegalArgumentException e) {
            return "redirect:/unidades/editar/" + id + "?erro=exclusao";
        }
    }

    private void prepararDetalhe(Model model, Unidade unidade) {
        model.addAttribute("unidade", unidade);
        model.addAttribute(
                "usuariosVinculados",
                unidadeService.listarUsuariosVinculados(unidade.getId())
        );
        model.addAttribute(
                "usuariosDisponiveis",
                unidadeService.listarUsuariosDisponiveis()
        );
        model.addAttribute("totalFuncionarios", unidadeService.contarFuncionarios(unidade.getId()));
        model.addAttribute("totalFlanges", unidadeService.contarFlanges(unidade.getId()));
        model.addAttribute("totalManutencoes", unidadeService.contarManutencoes(unidade.getId()));
        model.addAttribute("historicoUnidade", unidadeService.buscarHistorico(unidade.getId()));
    }

    private String carregarDetalheComErros(Unidade unidade, Model model) {
        prepararDetalhe(model, unidade);
        model.addAttribute("erroSalvar", "Confira os campos destacados e tente novamente.");
        return "unidade/editar";
    }
}
