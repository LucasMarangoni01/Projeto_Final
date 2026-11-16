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

    const statusSelect = document.getElementById("status");
    const limiteField = document.getElementById("temperaturaLimitField");
    const limiteInput = document.getElementById("limiteTemperaturaAlerta");

    function atualizarCampoLimiteTermico() {
        if (!tipoSelect || !statusSelect || !limiteField || !limiteInput) {
            return;
        }

        const sensorTermico = tipoSelect.value === "TEMPERATURA_SUPERFICIE";
        const instalado = statusSelect.value === "INSTALADO";
        const exigirLimite = sensorTermico && instalado;

        limiteField.hidden = !sensorTermico;
        limiteInput.required = exigirLimite;

        if (!sensorTermico) {
            limiteInput.value = "";
        }
    }

    if (tipoSelect && statusSelect) {
        tipoSelect.addEventListener("change", atualizarCampoLimiteTermico);
        statusSelect.addEventListener("change", atualizarCampoLimiteTermico);
        atualizarCampoLimiteTermico();
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
