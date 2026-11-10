package com.pbe.soda_caustica_flanges.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Garante que contas antigas também possuam um cadastro operacional de funcionário.
 *
 * Novas contas já são sincronizadas pelo UsuarioService.cadastrar().
 * Esta rotina cobre contas criadas antes dessa integração ou por fluxos antigos.
 */
@Component
public class SincronizacaoFuncionariosInicializacao implements ApplicationRunner {

    private final UsuarioService usuarioService;

    public SincronizacaoFuncionariosInicializacao(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int criados = usuarioService.sincronizarFuncionariosExistentes();
        if (criados > 0) {
            System.out.println(
                    "[SINCRONIZACAO] " + criados
                            + " conta(s) vinculada(s) automaticamente a funcionário(s)."
            );
        }
    }
}
