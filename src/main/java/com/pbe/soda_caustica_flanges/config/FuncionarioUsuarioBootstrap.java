package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Garante que contas criadas antes da associação automática também possuam
 * um cadastro de funcionário operacional.
 *
 * A sincronização é idempotente: uma conta já vinculada não gera duplicidade.
 */
@Component
public class FuncionarioUsuarioBootstrap {

    private static final Logger log =
            LoggerFactory.getLogger(FuncionarioUsuarioBootstrap.class);

    private final UsuarioService usuarioService;

    public FuncionarioUsuarioBootstrap(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarContasExistentes() {
        int total = usuarioService.sincronizarFuncionariosExistentes();

        if (total > 0) {
            log.info(
                    "Sincronização concluída: {} conta(s) vinculada(s) a funcionários operacionais.",
                    total
            );
        } else {
            log.info("Funcionários operacionais já estão sincronizados com as contas existentes.");
        }
    }
}
