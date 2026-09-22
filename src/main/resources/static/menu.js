// ==============================
// ABRIR MODAL
// ==============================

function abrirModalLogout() {

    const modal = document.getElementById("logoutModal");

    if (!modal) {
        return;
    }

    modal.hidden = false;

    document.body.style.overflow = "hidden";
}


// ==============================
// FECHAR MODAL
// ==============================

function fecharModalLogout(event) {

    if (
        event &&
        event.target !== event.currentTarget
    ) {
        return;
    }

    const modal = document.getElementById("logoutModal");

    if (!modal) {
        return;
    }

    modal.hidden = true;

    document.body.style.overflow = "";
}


// ==============================
// CONFIRMAR LOGOUT
// ==============================

function confirmarLogout() {

    const logoutForm =
        document.getElementById("logoutForm");

    if (!logoutForm) {
        return;
    }

    logoutForm.submit();
}


// ==============================
// TECLA ESC
// ==============================

document.addEventListener(
    "keydown",
    function(event) {

        if (event.key === "Escape") {

            fecharModalLogout();

        }

    }
);