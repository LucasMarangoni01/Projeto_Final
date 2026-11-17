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
