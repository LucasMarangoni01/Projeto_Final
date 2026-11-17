package com.pbe.soda_caustica_flanges;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SodaCausticaFlangesApplicationTests {
    // Um teste unitario sem MySQL; os testes de integracao devem ser feitos separadamente.
    @Test
    void aplicacaoPossuiPontoDeEntradaSpringBoot() {
        assertNotNull(SodaCausticaFlangesApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
