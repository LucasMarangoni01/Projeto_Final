package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    public String listar(
            Model model,
            @RequestParam(required = false) String sucesso,
            @RequestParam(required = false) String erro,
            @RequestParam(required = false) String busca) {

        // Os indicadores gerais continuam considerando todas as unidades.
        var todasUnidades = unidadeService.listarResumos();
        String termo = normalizarBusca(busca);

        var unidadesFiltradas = termo.isBlank()
                ? todasUnidades
                : todasUnidades.stream()
                        .filter(resumo -> correspondeBusca(resumo, termo))
                        .toList();

        model.addAttribute("unidadesResumo", unidadesFiltradas);
        // O painel lateral compartilha o mesmo filtro para localizar autores e unidades.
        model.addAttribute("registrosCadastro", unidadesFiltradas);
        model.addAttribute("totalUnidadesGeral", todasUnidades.size());
        model.addAttribute("busca", busca == null ? "" : busca);
        model.addAttribute("totalUnidadesAtivas", todasUnidades.stream()
                .filter(resumo -> resumo.unidade().isAtivo())
                .count());
        model.addAttribute("totalUsuariosCadastrados", todasUnidades.stream()
                .mapToLong(resumo -> resumo.totalUsuarios())
                .sum());
        model.addAttribute("totalFuncionariosCadastrados", todasUnidades.stream()
                .mapToLong(resumo -> resumo.totalFuncionarios())
                .sum());
        model.addAttribute("sucesso", sucesso);
        model.addAttribute("erro", erro);
        return "unidade/listagem";
    }

    private boolean correspondeBusca(
            com.pbe.soda_caustica_flanges.model.UnidadeResumoDTO resumo,
            String termoNormalizado) {

        Unidade unidade = resumo.unidade();
        String dados = String.join(" ",
                valorBusca(unidade.getId()),
                valorBusca(unidade.getNome()),
                valorBusca(unidade.getCodigo()),
                valorBusca(unidade.getLocalizacao()),
                valorBusca(unidade.getResponsavel()),
                valorBusca(unidade.getTelefoneContato()),
                valorBusca(unidade.getEmailContato()),
                valorBusca(unidade.getDescricao()),
                valorBusca(unidade.getCriadoEm()),
                valorBusca(resumo.criadoPorNome()),
                valorBusca(resumo.criadoPorEmail()),
                valorBusca(resumo.totalUsuarios()),
                valorBusca(resumo.totalFuncionarios()),
                valorBusca(resumo.totalFlanges()),
                valorBusca(resumo.totalManutencoes()),
                unidade.isAtivo() ? "ativa ativo" : "arquivada inativa"
        );

        return normalizarBusca(dados).contains(termoNormalizado);
    }

    private String valorBusca(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private String normalizarBusca(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        return java.text.Normalizer.normalize(
                        valor.trim(),
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT);
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
            model.addAttribute("erroSalvar", "Confira os campos destacados. O cadastro não foi salvo porque há informações inválidas.");
            return unidade.getId() == null ? "unidade/cadastro" : carregarDetalheComErros(unidade, model);
        }

        try {
            Unidade salva = unidadeService.salvar(unidade, authentication.getName());
            return "redirect:/unidades/editar/" + salva.getId() + "?sucesso=salva";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erroSalvar", e.getMessage());
            model.addAttribute("modoEdicao", unidade.getId() != null);
            return unidade.getId() == null ? "unidade/cadastro" : carregarDetalheComErros(unidade, model);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute(
                    "erroSalvar",
                    "O banco recusou o cadastro. Verifique se o código interno já está em uso e confira os dados obrigatórios."
            );
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

    @PostMapping("/adicionar-funcionario")
    public String adicionarFuncionario(
            @RequestParam Long unidadeId,
            @RequestParam String identificador,
            Authentication authentication) {
        try {
            unidadeService.adicionarFuncionario(
                    unidadeId,
                    identificador,
                    authentication.getName()
            );
            return "redirect:/unidades/editar/" + unidadeId + "?sucesso=funcionario-adicionado";
        } catch (IllegalArgumentException e) {
            return "redirect:/unidades/editar/" + unidadeId + "?erro=funcionario";
        }
    }

    @PostMapping("/remover-funcionario")
    public String removerFuncionario(
            @RequestParam Long unidadeId,
            @RequestParam Long funcionarioId,
            Authentication authentication) {
        try {
            unidadeService.removerFuncionario(
                    unidadeId,
                    funcionarioId,
                    authentication.getName()
            );
            return "redirect:/unidades/editar/" + unidadeId + "?sucesso=funcionario-removido";
        } catch (IllegalArgumentException e) {
            return "redirect:/unidades/editar/" + unidadeId + "?erro=funcionario";
        }
    }

    @PostMapping("/adicionar-flange")
    public String adicionarFlange(
            @RequestParam Long unidadeId,
            @RequestParam String identificador,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            unidadeService.adicionarFlange(
                    unidadeId,
                    identificador,
                    authentication.getName()
            );
            return "redirect:/unidades/editar/" + unidadeId + "?sucesso=flange-adicionada";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroDetalhe", e.getMessage());
            return "redirect:/unidades/editar/" + unidadeId + "?erro=flange";
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
        model.addAttribute(
                "funcionariosVinculados",
                unidadeService.listarFuncionariosVinculados(unidade.getId())
        );
        model.addAttribute(
                "funcionariosDisponiveis",
                unidadeService.listarFuncionariosDisponiveis()
        );
        model.addAttribute("totalFuncionarios", unidadeService.contarFuncionarios(unidade.getId()));
        model.addAttribute("totalFlanges", unidadeService.contarFlanges(unidade.getId()));
        model.addAttribute("flangesVinculadas", unidadeService.listarFlangesVinculadas(unidade.getId()));
        model.addAttribute("flangesDisponiveis", unidadeService.listarFlangesDisponiveis(unidade.getId()));
        model.addAttribute("totalManutencoes", unidadeService.contarManutencoes(unidade.getId()));
        model.addAttribute("historicoUnidade", unidadeService.buscarHistorico(unidade.getId()));
    }

    private String carregarDetalheComErros(Unidade unidade, Model model) {
        prepararDetalhe(model, unidade);
        model.addAttribute("erroSalvar", "Confira os campos destacados e tente novamente.");
        return "unidade/editar";
    }
}
