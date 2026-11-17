package com.pbe.soda_caustica_flanges;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThemeResourcesTest {
    private String resource(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Arquivo ausente: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void arquivosCompartilhadosEstaoDisponiveis() throws Exception {
        assertTrue(resource("static/theme.js").contains("flanges-theme"));
        assertTrue(resource("static/theme.css").contains("data-theme"));
    }

    @Test
    void todasAsPaginasCarregamTemaUmaUnicaVez() throws Exception {
        List<String> paginas = List.of(
            "alerta/listagem", "analises/index", "cadastro/cadastro",
            "dashboard/index_dashboard", "flange/alterar", "flange/cadastro",
            "flange/dashboard", "flange/listagem",
            "funcionario/alterar", "funcionario/cadastro", "funcionario/listagem",
            "guia", "home/index", "login/login",
            "manutencao/alterar", "manutencao/cadastro", "manutencao/listagem",
            "perfil/perfil", "sensor/gestao",
            "unidade/cadastro", "unidade/editar", "unidade/listagem",
            "usuario/editar", "usuario/listagem"
        );
        for (String pagina : paginas) {
            String html = resource("templates/" + pagina + ".html");
            assertEquals(1, html.split("src=\"/theme.js\"", -1).length - 1,
                "Script de tema precisa aparecer uma vez: " + pagina);
            assertEquals(1, html.split("href=\"/theme.css\"", -1).length - 1,
                "CSS de tema precisa aparecer uma vez: " + pagina);
        }
    }
}
