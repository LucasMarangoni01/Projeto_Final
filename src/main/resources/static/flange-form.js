document.addEventListener("DOMContentLoaded", () => {
    const unidadeSelect = document.getElementById("unidadeId");
    const localizacaoInput = document.getElementById("localizacao");

    if (!localizacaoInput || !unidadeSelect) {
        return;
    }

    const preencherLocalizacao = () => {
        const opcaoSelecionada = unidadeSelect.selectedOptions[0];

        if (!opcaoSelecionada || !unidadeSelect.value) {
            localizacaoInput.value = "";
            return;
        }

        localizacaoInput.value = opcaoSelecionada.dataset.localizacao || "";
    };

    unidadeSelect.addEventListener("change", preencherLocalizacao);
    preencherLocalizacao();
});
