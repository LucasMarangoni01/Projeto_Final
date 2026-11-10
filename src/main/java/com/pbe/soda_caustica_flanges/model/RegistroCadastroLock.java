package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro singleton utilizado para serializar os cadastros iniciais.
 * A linha de ID 1 é criada automaticamente na inicialização da aplicação.
 */
@Entity
@Table(name = "registro_cadastro_lock")
public class RegistroCadastroLock {

    @Id
    private Long id;

    protected RegistroCadastroLock() {
    }
}
