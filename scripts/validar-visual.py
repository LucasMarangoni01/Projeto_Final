#!/usr/bin/env python3
"""Verificacao estatica das rotas/elementos visuais criticos do FlangeGuard.

Nao substitui teste em navegador. Mantem os links originais de exportacao,
permissoes Thymeleaf e estruturas de pagina durante refinamentos visuais.
"""
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEMPLATES = ROOT / "src/main/resources/templates"
STATIC = ROOT / "src/main/resources/static"


class Markup(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.h1 = 0
        self.main = 0
        self.links = []
        self.classes = []
        self.errors = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "h1":
            self.h1 += 1
        elif tag == "main":
            self.main += 1
        if tag == "a":
            self.links.append(attrs.get("th:href", attrs.get("href", "")))
        for name in attrs.get("class", "").split():
            self.classes.append(name)


def parse(path):
    parser = Markup()
    parser.feed(path.read_text(encoding="utf-8"))
    parser.close()
    return parser


def validar():
    paginas = [
        "home/index.html",
        "flange/listagem.html",
        "funcionario/listagem.html",
        "funcionario/cadastro.html",
        "funcionario/alterar.html",
        "alerta/listagem.html",
        "analises/index.html",
        "relatorios/index.html",
        "guia.html",
    ]
    for arquivo in paginas:
        path = TEMPLATES / arquivo
        markup = parse(path)
        assert markup.main == 1, f"{arquivo}: esperado um elemento main"
        assert markup.h1 >= 1, f"{arquivo}: titulo principal h1 ausente"
        assert "theme.css" in path.read_text(encoding="utf-8"), f"{arquivo}: tema ausente"

    relatorios = parse(TEMPLATES / "relatorios/index.html")
    tipos = ("flanges", "funcionarios", "manutencoes", "ocorrencias", "alertas", "leituras")
    for tipo in tipos:
        assert any(f"/relatorios/exportar/{tipo}" in link for link in relatorios.links), (
            f"Exportacao {tipo} ausente"
        )

    # O Guia deve ter exatamente a mesma navegacao autenticada de outras telas,
    # preservando sua barra contextual, indice lateral, busca e impressao.
    guia = (TEMPLATES / "guia.html").read_text(encoding="utf-8")
    assert 'th:replace="~{fragments/menu :: menu}"' in guia, (
        "Guia sem menu compartilhado com o dashboard"
    )
    for elemento in ('id="guideSidebar"', 'id="openMenu"', 'id="printGuide"',
                     'id="guideSearch"', 'id="mobileOverlay"'):
        assert elemento in guia, f"Guia perdeu controle {elemento}"
    for arquivo in ('menu.css', 'menu.js', 'theme.css', 'theme.js', 'guia-nav.css'):
        assert arquivo in guia, f"Guia sem o recurso compartilhado {arquivo}"
    assert 'class="brand" href="#visao-geral"' not in guia, (
        "Guia voltou a apresentar menu superior duplicado"
    )

    menu = (TEMPLATES / "fragments/menu.html").read_text(encoding="utf-8")
    for rota in ("/flange/listagem", "/alertas", "/sensores", "/analises",
                 "/relatorios", "/funcionario/listagem", "/manutencao/listagem",
                 "/manutencao/cadastro", "/usuarios", "/unidades", "/guia"):
        assert rota in menu, f"Navegacao perdeu acesso a {rota}"
    assert "usuarioLogado.role.name() == 'ADMIN'" in menu, "Permissoes do menu ausentes"

    tema = (STATIC / "theme.css").read_text(encoding="utf-8")
    report = (STATIC / "relatorios.css").read_text(encoding="utf-8")
    for nome, css in (("theme.css", tema), ("relatorios.css", report),
                      ("guia-nav.css", (STATIC / "guia-nav.css").read_text(encoding="utf-8"))):
        assert css.count("{") == css.count("}"), f"{nome}: blocos CSS desbalanceados"
    assert 'html[data-theme="light"]' in tema and 'html[data-theme="dark"]' in tema
    # Regressões reais reportadas: barra azul com texto escuro e
    # formulários azuis com títulos invisíveis quando o tema está claro.
    assert 'html[data-theme="light"] .main-nav' in tema
    # A cor final do menu deve ser cinza-azulada, e nao o branco
    # introduzido pela revisao anterior. A ordem importa no CSS.
    assert 'background:#ffffff !important' in tema
    assert tema.rfind('background:#dbe3ed !important') > tema.rfind(
        'background:#ffffff !important'
    )
    assert 'FLANGEGUARD — MODO CLARO CINZA-ACO' in tema
    assert 'background:#cfd8e4' in tema
    assert 'fill:#cedae8' in tema
    # O mapa vetorial precisa de classes que respeitem as duas paletas;
    # as cores originais continuam funcionando no modo escuro.
    dashboard = (TEMPLATES / "home/index.html").read_text(encoding="utf-8")
    for classe in ("plant-background", "plant-pipe", "plant-pipe-detail",
                   "plant-node-name", "plant-node-status", "plant-footer"):
        assert f'class="{classe}"' in dashboard, (
            f"Mapa sem identidade tematizavel: {classe}"
        )
    assert 'html[data-theme="light"] body:has(.main-nav) main .card' in tema
    assert 'html[data-theme="light"] body:has(.main-nav) main .form-group label' in tema
    assert 'background:#fff8e8' in tema
    # Menus não podem permanecer simultaneamente expandidos.
    menu_js = (STATIC / "menu.js").read_text(encoding="utf-8")
    assert 'other.open = false' in menu_js
    assert 'event.key !== "Escape"' in menu_js
    # A área de FAQ empurra as demais perguntas ao expandir.
    guia_css = (STATIC / "guia-nav.css").read_text(encoding="utf-8")
    assert 'details.faq-item[open]>p' in guia_css
    assert 'position:static' in guia_css
    # Administradores podem atualizar os próprios dados profissionais
    # sem poder modificar seu papel de acesso nem a unidade.
    perfil = (TEMPLATES / "perfil/perfil.html").read_text(encoding="utf-8")
    assert 'th:action="@{/perfil/dados-profissionais}"' in perfil
    assert 'id="dados-profissionais"' in perfil
    assert 'th:object="${dadosProfissionaisDTO}"' in perfil

    assert '.relatorios-card' in report and '.relatorios-summary' in report
    print("Validação estática de interface: páginas, rotas, estrutura e CSS OK.")


if __name__ == "__main__":
    validar()
