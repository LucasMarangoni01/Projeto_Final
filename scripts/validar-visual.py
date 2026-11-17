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

    menu = (TEMPLATES / "fragments/menu.html").read_text(encoding="utf-8")
    for rota in ("/flange/listagem", "/alertas", "/sensores", "/analises",
                 "/relatorios", "/funcionario/listagem", "/manutencao/listagem",
                 "/manutencao/cadastro", "/usuarios", "/unidades", "/guia"):
        assert rota in menu, f"Navegacao perdeu acesso a {rota}"
    assert "usuarioLogado.role.name() == 'ADMIN'" in menu, "Permissoes do menu ausentes"

    tema = (STATIC / "theme.css").read_text(encoding="utf-8")
    report = (STATIC / "relatorios.css").read_text(encoding="utf-8")
    for nome, css in (("theme.css", tema), ("relatorios.css", report)):
        assert css.count("{") == css.count("}"), f"{nome}: blocos CSS desbalanceados"
    assert 'html[data-theme="light"]' in tema and 'html[data-theme="dark"]' in tema
    assert '.relatorios-card' in report and '.relatorios-summary' in report
    print("Validação estática de interface: páginas, rotas, estrutura e CSS OK.")


if __name__ == "__main__":
    validar()
