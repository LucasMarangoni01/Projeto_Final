package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flanges")
public class FlangeRestController {

    private final FlangeRepository flangeRepository;
    private final UsuarioRepository usuarioRepository;

    public FlangeRestController(
            FlangeRepository flangeRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.flangeRepository = flangeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<FlangeBuscaResponse>> buscarFlanges(
            @RequestParam("q") String termo,
            Authentication authentication
    ) {
        if (termo == null || termo.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow();

        List<Flange> resultados;

        if (usuario.getRole() == Role.ADMIN) {
            resultados = flangeRepository.buscarPorTermo(
                    termo.trim(),
                    PageRequest.of(0, 5)
            );
        } else if (usuario.getUnidade() == null) {
            resultados = List.of();
        } else {
            resultados = flangeRepository.buscarPorTermoEUnidade(
                    termo.trim(),
                    usuario.getUnidade().getId(),
                    PageRequest.of(0, 5)
            );
        }

        List<FlangeBuscaResponse> resposta = resultados.stream()
                .map(flange -> new FlangeBuscaResponse(
                        flange.getId(),
                        flange.getNome_flange()
                ))
                .toList();

        return ResponseEntity.ok(resposta);
    }

    public record FlangeBuscaResponse(Long id, String nome_flange) {
    }
}
