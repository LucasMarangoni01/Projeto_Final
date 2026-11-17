package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.AlterarSenhaDTO;
import com.pbe.soda_caustica_flanges.model.DadosProfissionaisDTO;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.PerfilDTO;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.FotoPerfilService;
import com.pbe.soda_caustica_flanges.service.UsuarioAuditoriaService;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;

@Controller
public class PerfilController {

    private final UsuarioService usuarioService;
    private final FotoPerfilService fotoPerfilService;
    private final UsuarioAuditoriaService auditoriaService;

    public PerfilController(
            UsuarioService usuarioService,
            FotoPerfilService fotoPerfilService,
            UsuarioAuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.fotoPerfilService = fotoPerfilService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/perfil")
    public String perfil(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) String perfil,
            @RequestParam(required = false) String senha,
            @RequestParam(required = false) String dados) {

        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());
        prepararTela(model, usuario);
        model.addAttribute("statusPerfil", perfil);
        model.addAttribute("statusSenha", senha);
        model.addAttribute("statusDadosProfissionais", dados);
        return "perfil/perfil";
    }

    @PostMapping("/perfil/atualizar")
    public String atualizarPerfil(
            @Valid @ModelAttribute("perfilDTO") PerfilDTO perfilDTO,
            BindingResult result,
            @RequestParam(name = "fotoPerfil", required = false) MultipartFile foto,
            @RequestParam(name = "removerFoto", defaultValue = "false") boolean removerFoto,
            Authentication authentication,
            Model model) {

        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());

        if (result.hasErrors()) {
            prepararTela(model, usuario);
            model.addAttribute("perfilDTO", perfilDTO);
            model.addAttribute("erroPerfil", "Confira os campos destacados e tente novamente.");
            return "perfil/perfil";
        }

        String fotoAnterior = usuario.getFotoPerfil();
        String novaFoto = null;

        try {
            if (foto != null && !foto.isEmpty()) {
                novaFoto = fotoPerfilService.salvar(foto);
            }

            Usuario atualizado = usuarioService.atualizarPerfil(
                    authentication.getName(),
                    perfilDTO,
                    novaFoto,
                    removerFoto
            );

            if ((novaFoto != null || removerFoto)
                    && fotoAnterior != null
                    && !fotoAnterior.equals(atualizado.getFotoPerfil())) {
                removerFotoAntiga(fotoAnterior);
            }

            return "redirect:/perfil?perfil=atualizado";

        } catch (IllegalArgumentException e) {
            removerFotoNova(novaFoto);
            prepararTela(model, usuario);
            model.addAttribute("perfilDTO", perfilDTO);
            model.addAttribute("erroPerfil", e.getMessage());
            return "perfil/perfil";

        } catch (IOException e) {
            removerFotoNova(novaFoto);
            prepararTela(model, usuario);
            model.addAttribute("perfilDTO", perfilDTO);
            model.addAttribute("erroPerfil", "Não foi possível processar a foto. Envie JPEG ou PNG de até 2 MB.");
            return "perfil/perfil";
        }
    }

    @PostMapping("/perfil/alterar-senha")
    public String alterarSenha(
            @Valid @ModelAttribute("alterarSenhaDTO") AlterarSenhaDTO dados,
            BindingResult result,
            Authentication authentication,
            Model model) {

        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());

        if (result.hasErrors()) {
            prepararTela(model, usuario);
            model.addAttribute("erroSenha", "Confira os campos. A nova senha deve ter pelo menos 12 caracteres.");
            return "perfil/perfil";
        }

        try {
            usuarioService.alterarSenha(authentication.getName(), dados);
            return "redirect:/perfil?senha=alterada";
        } catch (IllegalArgumentException e) {
            prepararTela(model, usuario);
            model.addAttribute("erroSenha", e.getMessage());
            return "perfil/perfil";
        }
    }

    /**
     * Permite que o ADMIN edite os proprios dados profissionais, sem alterar
     * permissao, unidade, email nem status de acesso. O ID vem da sessao.
     */
    @PostMapping("/perfil/dados-profissionais")
    public String atualizarDadosProfissionais(
            @Valid @ModelAttribute("dadosProfissionaisDTO") DadosProfissionaisDTO dados,
            BindingResult result,
            Authentication authentication,
            Model model) {

        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());
        if (usuario.getRole() != Role.ADMIN || !usuario.isAtivo()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (result.hasErrors()) {
            prepararTela(model, usuario);
            model.addAttribute("dadosProfissionaisDTO", dados);
            model.addAttribute("erroDadosProfissionais",
                    "Confira os limites de caracteres de matrícula, cargo e setor.");
            return "perfil/perfil";
        }

        try {
            usuarioService.alterarDadosProfissionais(
                    usuario.getId(), dados, authentication.getName());
            return "redirect:/perfil?dados=atualizados#dados-profissionais";
        } catch (IllegalArgumentException e) {
            prepararTela(model, usuario);
            model.addAttribute("dadosProfissionaisDTO", dados);
            model.addAttribute("erroDadosProfissionais", e.getMessage());
            return "perfil/perfil";
        }
    }

    @GetMapping("/perfil/foto/{nomeArquivo:.+}")
    @ResponseBody
    public ResponseEntity<byte[]> fotoPerfil(
            @PathVariable String nomeArquivo,
            Authentication authentication) {
        if (authentication == null
                || !usuarioService.podeVisualizarFoto(nomeArquivo, authentication.getName())) {
            return ResponseEntity.notFound().build();
        }

        try {
            FotoPerfilService.FotoCarregada foto = fotoPerfilService.carregar(nomeArquivo);
            return ResponseEntity.ok()
                    .contentType(foto.contentType())
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(foto.bytes());
        } catch (FileNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void prepararTela(Model model, Usuario usuario) {
        model.addAttribute("usuarioLogado", usuario);
        model.addAttribute("perfilDTO", new PerfilDTO(usuario));
        model.addAttribute("alterarSenhaDTO", new AlterarSenhaDTO());
        model.addAttribute("dadosProfissionaisDTO", new DadosProfissionaisDTO(usuario));
        model.addAttribute("historicoUsuario", auditoriaService.buscarPorUsuario(usuario.getId()));
    }

    private void removerFotoAntiga(String nomeArquivo) {
        try {
            fotoPerfilService.remover(nomeArquivo);
        } catch (IOException ignored) {
            // Falha na limpeza do arquivo antigo não desfaz a atualização salva no banco.
        }
    }

    private void removerFotoNova(String nomeArquivo) {
        if (nomeArquivo != null) {
            removerFotoAntiga(nomeArquivo);
        }
    }
}
