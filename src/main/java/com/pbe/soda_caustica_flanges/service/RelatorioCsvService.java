package com.pbe.soda_caustica_flanges.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Relatorios CSV UTF-8 compatíveis com planilhas brasileiras.
 * Textos de usuarios nunca sao tratados como formulas da planilha.
 */
@Service
public class RelatorioCsvService {

    public byte[] gerar(List<String> cabecalho, List<List<String>> registros) {
        StringBuilder resultado = new StringBuilder("\uFEFF");
        adicionarLinha(resultado, cabecalho);
        for (List<String> registro : registros) {
            if (registro.size() != cabecalho.size()) {
                throw new IllegalArgumentException("Numero de colunas inconsistente no relatorio.");
            }
            adicionarLinha(resultado, registro);
        }
        return resultado.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void adicionarLinha(StringBuilder saida, List<String> valores) {
        for (int coluna = 0; coluna < valores.size(); coluna++) {
            if (coluna > 0) {
                saida.append(';');
            }
            saida.append('"').append(protegerCelula(valores.get(coluna))).append('"');
        }
        saida.append("\r\n");
    }

    static String protegerCelula(String valor) {
        if (valor == null) {
            return "";
        }
        String seguro = valor.replace("\r", " ").replace("\n", " ");
        String inicio = seguro.stripLeading();
        if (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        return seguro.replace("\"", "\"\"");
    }
}
