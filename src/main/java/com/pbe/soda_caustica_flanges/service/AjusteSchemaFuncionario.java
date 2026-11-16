package com.pbe.soda_caustica_flanges.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajusta bancos legados para permitir funcionários sem data de nascimento.
 *
 * A data é opcional para funcionários criados automaticamente a partir de uma
 * conta de usuário. O ALTER mantém todos os valores já gravados e apenas remove
 * a obrigatoriedade da coluna no MySQL.
 *
 * Executa antes da sincronização automática de usuários e funcionários.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AjusteSchemaFuncionario implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AjusteSchemaFuncionario(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
                "ALTER TABLE funcionario MODIFY COLUMN data_nascimento DATE NULL"
        );
    }
}
