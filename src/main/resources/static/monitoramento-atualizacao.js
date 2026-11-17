(function () {
    'use strict';

    const area = document.getElementById('monitoramento-auto');
    if (!area) return;

    const indicador = document.getElementById('monitoramento-status');
    const aviso = document.getElementById('monitoramento-novidade');
    const badge = document.getElementById('badge');
    let referencia = null;
    let consultando = false;

    async function consultar() {
        if (consultando || document.hidden) return;
        consultando = true;
        try {
            const resposta = await fetch('/api/monitoramento/resumo', {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: {'Accept': 'application/json'}
            });
            if (!resposta.ok) throw new Error('Consulta indisponível');
            const dados = await resposta.json();
            const estado = JSON.stringify([dados.alertasAtivos, dados.ultimaLeituraEm]);
            if (referencia !== null && referencia !== estado) {
                if (badge) {
                    const total = Number(dados.alertasAtivos) || 0;
                    badge.textContent = total === 0
                        ? 'Nenhum alerta ativo'
                        : total + (total === 1 ? ' alerta ativo' : ' alertas ativos');
                    badge.classList.toggle('al-b', total > 0);
                    badge.classList.toggle('ok-b', total === 0);
                }
                if (aviso) {
                    aviso.hidden = false;
                    aviso.textContent = Number(dados.alertasAtivos) > 0
                        ? 'Novas informações de monitoramento. Abrir painel atualizado'
                        : 'Dados atualizados. Recarregar painel';
                }
            }
            referencia = estado;
            if (indicador) indicador.textContent =
                'Consulta periódica ativa. Última verificação: ' + new Date().toLocaleTimeString('pt-BR');
        } catch (e) {
            if (indicador) {
                indicador.textContent = 'Não foi possível consultar atualizações. Verifique a conexão.';
            }
        } finally {
            consultando = false;
        }
    }

    consultar();
    setInterval(consultar, 7000);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) consultar();
    });
})();
