package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
public class ManutencaoController {

    private final ManutencaoRepository manutencaoRepository;
    private final FlangeRepository flangeRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioRepository usuarioRepository;

    public ManutencaoController(
            ManutencaoRepository manutencaoRepository,
            FlangeRepository flangeRepository,
            FuncionarioRepository funcionarioRepository,
            UsuarioRepository usuarioRepository) {
        this.manutencaoRepository = manutencaoRepository;
        this.flangeRepository = flangeRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/manutencao/listagem")
    public String listagem(Model model, Authentication authentication) {
        Usuario usuario = buscarUsuarioLogado(authentication);
        boolean admin = usuario.getRole() == Role.ADMIN;

        List<Manutencao> manutencoes;
        if (admin) {
            manutencoes = manutencaoRepository.findAllByOrderByDataDesc();
        } else if (usuario.getUnidade() == null) {
            manutencoes = Collections.emptyList();
        } else {
            manutencoes = manutencaoRepository.findByFlange_Unidade_IdOrderByDataDesc(
                    usuario.getUnidade().getId()
            );
        }

        model.addAttribute("manutencoes", manutencoes);
        model.addAttribute("ehAdmin", admin);
        return "manutencao/listagem";
    }

    @GetMapping("/manutencao/cadastro")
    public String cadastro(Model model, Authentication authentication) {
        Usuario usuario = buscarUsuarioLogado(authentication);
        Manutencao manutencao = new Manutencao();
        manutencao.setData(new java.util.Date());
        prepararFormulario(model, manutencao, usuario);
        return "manutencao/cadastro";
    }

    @PostMapping("/manutencao/salvar")
    public String salvar(
            @ModelAttribute("manutencao") Manutencao manutencao,
            @RequestParam(value = "flangeId", required = false) Long flangeId,
            @RequestParam(value = "funcionarioId", required = false) Long funcionarioId,
            Authentication authentication,
            Model model) {

        Usuario usuario = buscarUsuarioLogado(authentication);
        boolean admin = usuario.getRole() == Role.ADMIN;

        if (!admin && manutencao.getId() != null) {
            return "redirect:/manutencao/listagem?erro=permissao";
        }

        if (flangeId == null || funcionarioId == null) {
            List<Funcionario> funcionariosDisponiveis = listarFuncionariosPermitidos(usuario);

            String mensagem = funcionariosDisponiveis.isEmpty()
                    ? mensagemSemFuncionarios(usuario)
                    : "Selecione uma flange e um funcionário responsável.";

            model.addAttribute("erro", mensagem);
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        Flange flange = flangeRepository.findById(flangeId).orElse(null);
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId).orElse(null);

        if (flange == null || funcionario == null) {
            model.addAttribute("erro", "A flange ou o funcionário selecionado não existe.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (flange.getUnidade() == null || funcionario.getUnidade() == null
                || !flange.getUnidade().getId().equals(funcionario.getUnidade().getId())) {
            model.addAttribute("erro", "A flange e o funcionário precisam pertencer à mesma unidade.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (!admin) {
            if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()
                    || !flange.getUnidade().getId().equals(usuario.getUnidade().getId())) {
                model.addAttribute("erro", "Você só pode registrar manutenções em ativos da sua unidade.");
                prepararFormulario(model, manutencao, usuario);
                return "manutencao/cadastro";
            }
        } else if (!flange.getUnidade().isAtivo() && manutencao.getId() == null) {
            model.addAttribute("erro", "Não é possível registrar uma nova manutenção em uma unidade arquivada.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (manutencao.getId() == null) {
            // Guarda quem realizou o lançamento, independentemente do funcionário executor.
            manutencao.setRegistradoPorEmail(authentication.getName());
        } else {
            // Uma correção administrativa não pode sobrescrever a autoria e a data originais.
            Manutencao registroExistente = manutencaoRepository.findById(manutencao.getId())
                    .orElse(null);

            if (registroExistente == null) {
                return "redirect:/manutencao/listagem";
            }

            manutencao.setRegistradoPorEmail(registroExistente.getRegistradoPorEmail());
            manutencao.setRegistradoEm(registroExistente.getRegistradoEm());
        }

        manutencao.setFlange(flange);
        manutencao.setFuncionario(funcionario);
        manutencaoRepository.save(manutencao);

        return "redirect:/manutencao/listagem";
    }

    @GetMapping("/manutencao/alterar/{id}")
    public String alterar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {
        Usuario usuario = buscarUsuarioLogado(authentication);
        Manutencao manutencao = manutencaoRepository.findById(id).orElse(null);

        if (manutencao == null) {
            return "redirect:/manutencao/listagem";
        }

        prepararFormulario(model, manutencao, usuario);
        return "manutencao/cadastro";
    }

    @PostMapping("/manutencao/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        manutencaoRepository.findById(id).ifPresent(manutencaoRepository::delete);
        return "redirect:/manutencao/listagem";
    }

    private Usuario buscarUsuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private void prepararFormulario(Model model, Manutencao manutencao, Usuario usuario) {
        boolean admin = usuario.getRole() == Role.ADMIN;
        List<Flange> flanges;

        if (admin) {
            flanges = flangeRepository.findAll();
        } else if (usuario.getUnidade() == null) {
            flanges = Collections.emptyList();
        } else {
            flanges = flangeRepository.findByUnidadeId(usuario.getUnidade().getId());
        }

        List<Funcionario> funcionarios = listarFuncionariosPermitidos(usuario);

        model.addAttribute("manutencao", manutencao);
        model.addAttribute("flanges", flanges);
        model.addAttribute("funcionarios", funcionarios);
        model.addAttribute("ehAdmin", admin);
        model.addAttribute("unidadeUsuario", usuario.getUnidade());
        model.addAttribute("nenhumFuncionarioDisponivel", funcionarios.isEmpty());
    }

    private List<Funcionario> listarFuncionariosPermitidos(Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return funcionarioRepository.findAll()
                    .stream()
                    .sorted(java.util.Comparator.comparing(
                            Funcionario::getNome_funcionario,
                            java.text.Collator.getInstance(new java.util.Locale("pt", "BR"))
                    ))
                    .toList();
        }

        if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            return Collections.emptyList();
        }

        return funcionarioRepository.findByUnidadeIdOrderByIdAsc(
                usuario.getUnidade().getId()
        );
    }

    private String mensagemSemFuncionarios(Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return "Não há funcionários operacionais cadastrados. Cadastre um funcionário antes de registrar a manutenção.";
        }

        String unidade = usuario.getUnidade() == null
                ? "sua unidade"
                : usuario.getUnidade().getNome();

        return "Não há funcionários operacionais vinculados a " + unidade
                + ". Solicite ao ADMIN que cadastre ou vincule um funcionário a essa unidade.";
    }
}
