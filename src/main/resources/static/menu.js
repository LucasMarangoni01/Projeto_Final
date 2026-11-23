function abrirModalLogout() {
    const modal = document.getElementById("logoutModal");

    if (!modal) {
        return;
    }

    modal.hidden = false;
    document.body.style.overflow = "hidden";
}

function fecharModalLogout(event) {
    if (event && event.target !== event.currentTarget) {
        return;
    }

    const modal = document.getElementById("logoutModal");

    if (!modal) {
        return;
    }

    modal.hidden = true;
    document.body.style.overflow = "";
}

function confirmarLogout() {
    const logoutForm = document.getElementById("logoutForm");

    if (!logoutForm) {
        return;
    }

    logoutForm.submit();
}

document.addEventListener("keydown", function(event) {
    if (event.key === "Escape") {
        fecharModalLogout();
    }
});

/**
 * Encerra a sessão atual e retorna à tela de login para autenticar
 * com outra conta. Não altera nem remove o cadastro existente.
 */
function trocarConta() {
    const switchAccountForm = document.getElementById("switchAccountForm");

    if (!switchAccountForm) {
        return;
    }

    switchAccountForm.submit();
}


/**
 * Navegacao ativa: destaca a area em que o usuario se encontra.
 * Mantem os menus responsivos e os links de permissoes do Thymeleaf.
 */
document.addEventListener("DOMContentLoaded", function() {
    const menu = document.querySelector(".main-nav");
    if (!menu) return;

    const caminhoAtual = window.location.pathname.replace(/\/+$/, "") || "/";
    const rotasDeEdicao = [
        [/^\/flange\//, "/flange/listagem"],
        [/^\/funcionario\//, "/funcionario/listagem"],
        [/^\/manutencao\//, "/manutencao/listagem"],
        [/^\/usuarios\//, "/usuarios"],
        [/^\/unidades\//, "/unidades"],
        [/^\/sensores\//, "/sensores"],
        [/^\/analises\//, "/analises"],
        [/^\/relatorios\//, "/relatorios"]
    ];
    const seletores = ".nav-link, .mobile-menu-link, .nav-dropdown-panel a";
    const links = Array.from(menu.querySelectorAll(seletores));
    const rotaExata = links.some(function(link) {
        return new URL(link.href, window.location.origin).pathname.replace(/\/+$/, "") === caminhoAtual;
    });
    let alvo = caminhoAtual;
    if (!rotaExata) {
        const regra = rotasDeEdicao.find(function(item) { return item[0].test(caminhoAtual); });
        if (regra) alvo = regra[1];
    }

    links.forEach(function(link) {
        const caminhoLink = new URL(link.href, window.location.origin).pathname.replace(/\/+$/, "") || "/";
        if (caminhoLink !== alvo) return;
        link.classList.add("is-current");
        link.setAttribute("aria-current", "page");
        const grupo = link.closest(".nav-dropdown");
        if (grupo) grupo.classList.add("has-current");
    });
});

/**
 * Menus expansíveis são mutuamente exclusivos: impede dois painéis
 * sobrepostos, fecha ao clicar fora ou pressionar Esc.
 */
document.addEventListener("DOMContentLoaded", function() {
    const menu = document.querySelector(".main-nav");
    if (!menu) return;

    const menus = Array.from(menu.querySelectorAll("details.nav-dropdown, details.mobile-menu"));
    menus.forEach(function(item) {
        item.addEventListener("toggle", function() {
            if (!item.open) return;
            menus.forEach(function(other) {
                if (other !== item) other.open = false;
            });
        });
    });

    document.addEventListener("pointerdown", function(event) {
        if (!menu.contains(event.target)) {
            menus.forEach(function(item) { item.open = false; });
            return;
        }
        menus.forEach(function(item) {
            if (item.open && !item.contains(event.target)) item.open = false;
        });
    });

    menu.addEventListener("keydown", function(event) {
        if (event.key !== "Escape") return;
        const aberto = menus.find(function(item) { return item.open; });
        if (!aberto) return;
        aberto.open = false;
        const summary = aberto.querySelector("summary");
        if (summary) summary.focus();
        event.preventDefault();
    });
});


/**
 * Badge leve de alertas registrados em toda a navegacao autenticada.
 * Usa resumo da unidade autorizada; simulacoes locais nunca entram na conta.
 */
document.addEventListener("DOMContentLoaded", function () {
    const links = Array.from(document.querySelectorAll(".main-nav .nav-link-alertas"));
    if (!links.length) return;
    let consultando = false;
    async function consultarAlertasDoMenu() {
        if (document.hidden || consultando) return;
        consultando = true;
        try {
            const resposta = await fetch("/api/monitoramento/resumo", {
                credentials: "same-origin", cache: "no-store",
                headers: {Accept: "application/json"}
            });
            if (!resposta.ok) throw new Error("Resumo temporariamente indisponível");
            const dados = await resposta.json();
            const numero = Math.max(0, Number(dados.alertasAtivos) || 0);
            links.forEach(function (link) {
                const contador = link.querySelector(".nav-alert-counter");
                if (contador) {
                    contador.hidden = numero === 0;
                    contador.textContent = numero > 99 ? "99+" : String(numero);
                }
                link.setAttribute("aria-label", numero > 0
                    ? ("Alertas e ocorrências: " + numero + " alertas registrados ativos")
                    : "Alertas e ocorrências");
                link.removeAttribute("data-update-failed");
            });
        } catch (e) {
            // Preservar último estado em falhas de rede: nao afirmar zero.
            links.forEach(function (link) {
                link.dataset.updateFailed = "true";
                link.title = "Consulta indisponível: último número registrado, se disponível";
            });
        } finally {
            consultando = false;
        }
    }
    consultarAlertasDoMenu();
    window.setInterval(consultarAlertasDoMenu, 7000);
    document.addEventListener("visibilitychange", function () {
        if (!document.hidden) consultarAlertasDoMenu();
    });
});
