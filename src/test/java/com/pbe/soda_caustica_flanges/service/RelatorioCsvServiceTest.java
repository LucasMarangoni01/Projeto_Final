package com.pbe.soda_caustica_flanges.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioCsvServiceTest {

    private final RelatorioCsvService service = new RelatorioCsvService();

    @Test
    void preservaAcentosESeparadoresNoCsvUtf8() {
        String csv = new String(service.gerar(
                List.of("Nome", "Ocorrencia"),
                List.of(List.of("Flange 01", "Inspecao; troca \"junta\" e revisão"))
        ), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"Inspecao; troca \"\"junta\"\" e revisão\""));
        assertTrue(csv.contains("\r\n"));
    }

    @Test
    void neutralizaFormulasEQuebrasDeLinhaInformadasPeloUsuario() {
        String csv = new String(service.gerar(
                List.of("Descricao", "Responsavel"),
                List.of(List.of(" =HYPERLINK(\"url\")\nproxima linha", "+EXEMPLO"))
        ), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"' =HYPERLINK("));
        assertTrue(csv.contains("proxima linha"));
        assertTrue(csv.contains("\"'+EXEMPLO\""));
        assertFalse(csv.contains("\nproxima linha"));
    }

    @Test
    void rejeitaRegistrosComNumeroDeColunasDiferente() {
        assertThrows(IllegalArgumentException.class,
                () -> service.gerar(List.of("A", "B"), List.of(List.of("1"))));
    }

    @Test
    void geraCabecalhoMesmoSemRegistros() {
        String csv = new String(service.gerar(List.of("A"), List.of()), StandardCharsets.UTF_8);
        assertEquals("\uFEFF\"A\"\r\n", csv);
    }
}
