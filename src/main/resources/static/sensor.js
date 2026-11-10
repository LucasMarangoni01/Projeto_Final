document.addEventListener("DOMContentLoaded", function () {
    const tipoSelect = document.getElementById("tipo");
    const tipoAjuda = document.getElementById("sensorTypeHelp");

    function atualizarFinalidadeTipo() {
        if (!tipoSelect || !tipoAjuda) {
            return;
        }

        const opcaoSelecionada = tipoSelect.options[tipoSelect.selectedIndex];
        const finalidade = opcaoSelecionada
            ? opcaoSelecionada.getAttribute("data-finalidade")
            : null;

        tipoAjuda.textContent = finalidade
            || "Selecione a grandeza ou condição acompanhada pelo dispositivo.";
    }

    if (tipoSelect) {
        tipoSelect.addEventListener("change", atualizarFinalidadeTipo);
        atualizarFinalidadeTipo();
    }

    const campoBusca = document.getElementById("sensorSearch");
    const linhasSensores = Array.from(document.querySelectorAll("[data-sensor-row]"));
    const linhaSemResultado = document.getElementById("sensorSearchEmpty");

    if (campoBusca) {
        campoBusca.addEventListener("input", function () {
            const termo = campoBusca.value.trim().toLocaleLowerCase("pt-BR");
            let encontrados = 0;

            linhasSensores.forEach(function (linha) {
                const corresponde = linha.textContent
                    .toLocaleLowerCase("pt-BR")
                    .includes(termo);

                linha.hidden = !corresponde;

                if (corresponde) {
                    encontrados++;
                }
            });

            if (linhaSemResultado) {
                linhaSemResultado.hidden = termo.length === 0 || encontrados > 0;
            }
        });
    }
});
