package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.RegistroCadastroLockRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prepara o bloqueio utilizado pelo cadastro e faz a transição de bancos
 * antigos: se ainda não existir um ADMIN ativo, o usuário ativo mais antigo
 * recebe a permissão administrativa.
 */
@Component
public class InicializacaoCadastro implements ApplicationRunner {

    private final RegistroCadastroLockRepository registroLockRepository;
    private final UsuarioRepository usuarioRepository;

    public InicializacaoCadastro(
            RegistroCadastroLockRepository registroLockRepository,
            UsuarioRepository usuarioRepository) {
        this.registroLockRepository = registroLockRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        registroLockRepository.garantirRegistroInicial();

        registroLockRepository.bloquearPorId(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Não foi possível preparar o controle do primeiro cadastro."
                ));

        if (usuarioRepository.countByRoleAndAtivoTrue(Role.ADMIN) > 0) {
            return;
        }

        usuarioRepository.findFirstByAtivoTrueOrderByIdAsc().ifPresent(usuario -> {
            usuario.setRole(Role.ADMIN);
            usuarioRepository.save(usuario);
            System.out.println(
                    "Primeiro usuário ativo promovido automaticamente para ADMIN: "
                            + usuario.getEmail()
            );
        });
    }
}
