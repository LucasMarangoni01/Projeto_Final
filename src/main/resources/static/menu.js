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
