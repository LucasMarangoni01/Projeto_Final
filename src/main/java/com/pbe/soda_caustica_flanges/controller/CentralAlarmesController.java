package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.service.CentralAlarmesService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoramento")
public class CentralAlarmesController {
    private final CentralAlarmesService service;

    public CentralAlarmesController(CentralAlarmesService service) { this.service = service; }

    @GetMapping("/alarmes")
    public ResponseEntity<CentralAlarmesService.Painel> painel(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.painel(auth));
    }

    public record Inicio(Long flangeId, TesteEmergencia.Tipo tipo, TesteEmergencia.Gravidade gravidade, boolean confirmado) {}

    @PostMapping("/testes")
    public ResponseEntity<AlarmePainelDTO> iniciar(Authentication auth, @RequestBody Inicio pedido) {
        return ResponseEntity.ok(service.iniciar(auth, pedido.flangeId(), pedido.tipo(), pedido.gravidade(), pedido.confirmado()));
    }

    @PostMapping("/testes/{id}/{acao}")
    public ResponseEntity<AlarmePainelDTO> transicao(Authentication auth, @PathVariable Long id, @PathVariable String acao) {
        return ResponseEntity.ok(service.transicao(auth, id, acao));
    }

    @PostMapping("/alarmes/{id}/atender")
    public ResponseEntity<Void> atender(Authentication auth, @PathVariable Long id) {
        service.atenderReal(auth, id);
        return ResponseEntity.noContent().build();
    }
}
