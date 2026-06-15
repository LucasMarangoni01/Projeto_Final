package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flanges")
public class FlangeRestController {

    @Autowired
    private FlangeRepository flangeRepository;

    @GetMapping("/buscar")
    public ResponseEntity<List<Flange>> buscarFlanges(@RequestParam("q") String termo) {

        // Faz a busca no banco passando o termo digitado e limita a resposta a 5 itens (PageRequest)
        List<Flange> resultados = flangeRepository.buscarPorTermo(termo, PageRequest.of(0, 5));

        // Retorna a lista de flanges em formato JSON com status 200 (OK)
        return ResponseEntity.ok(resultados);
    }
}