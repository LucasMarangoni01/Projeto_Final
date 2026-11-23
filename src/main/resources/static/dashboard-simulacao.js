/*
 * FlangeGuard: painel de monitoramento e demonstracao visual.
 * NAO envia requisicoes de escrita, NAO simula hardware, NAO persiste incidentes.
 */
(function () {
    'use strict';

    const board = document.getElementById('painel-monitoramento');
    if (!board) return;

    const feed = document.getElementById('monitorEventFeed');
    const overviewCount = document.getElementById('monitorActiveCount');
    const overviewTitle = document.getElementById('monitorOverviewTitle');
    const banner = document.getElementById('monitorCriticalBanner');
    const bannerTitle = document.getElementById('monitorCriticalTitle');
    const bannerDescription = document.getElementById('monitorCriticalDescription');
    const bannerTag = document.getElementById('monitorCriticalTag');
    const simSummary = document.getElementById('monitorSimSummary');
    const simTarget = document.getElementById('monitorSimTarget');
    const simScenario = document.getElementById('monitorSimScenario');
    const demoEvent = document.getElementById('monitorDemoEvent');
    const demoDescription = document.getElementById('monitorDemoDescription');
    const modal = document.getElementById('modalSimulacao');
    const openButton = document.getElementById('abrirSimulacao');
    const flangeSelect = document.getElementById('monitorSimFlange');
    const typeSelect = document.getElementById('monitorSimTipo');
    const flangeCards = Array.from(document.querySelectorAll('.dashboard-flange-card[data-flange-id]'));
    const mapFlanges = Array.from(document.querySelectorAll('.plant-flange[data-flange-id]'));

    let totalAtivos = Number(board.dataset.alertasReais || 0);
    let simulacao = null;
    let consultando = false;
    const original = new Map();

    function textoSeguro(valor) {
        return typeof valor === 'string' ? valor : '';
    }

    function criar(tag, classe, texto) {
        const elemento = document.createElement(tag);
        if (classe) elemento.className = classe;
        if (texto !== undefined) elemento.textContent = String(texto);
        return elemento;
    }

    function atualizarEventos(eventos) {
        if (!feed || !Array.isArray(eventos)) return;
        const fragmento = document.createDocumentFragment();

        if (eventos.length === 0) {
            fragmento.append(criar('p', 'monitor-empty',
                'Ainda não há ocorrências individuais registradas. A ausência de alertas não comprova segurança.'));
        } else {
            eventos.slice(0, 6).forEach(function (evento) {
                const item = criar('article', 'monitor-event ' + (evento.ativo ? 'is-active' : 'is-resolved'));
                const indicador = criar('span', 'monitor-event-indicator');
                indicador.setAttribute('aria-hidden', 'true');
                const corpo = criar('div', 'monitor-event-body');
                const topo = criar('div', 'monitor-event-top');
                const nome = criar('strong', '', textoSeguro(evento.nomeFlange) || 'Flange sem identificação');
                const horario = criar('time', '', textoSeguro(evento.dataHora) || '—');
                topo.append(nome, horario);
                corpo.append(topo,
                    criar('p', '', textoSeguro(evento.descricao) || 'Ocorrência em verificação'),
                    criar('span', 'monitor-event-status',
                        textoSeguro(evento.situacao) || (evento.ativo ? 'Alerta ativo' : 'Normalizado')));
                item.append(indicador, corpo);
                fragmento.append(item);
            });
        }
        feed.replaceChildren(fragmento);
    }

    function atualizarFlangesReais(ids) {
        const permitidos = new Set((Array.isArray(ids) ? ids : [])
            .map(String));
        flangeCards.concat(mapFlanges).forEach(function (elemento) {
            elemento.dataset.alarmActive = String(permitidos.has(elemento.dataset.flangeId));
        });
    }

    function atualizarBanner() {
        const simCritica = simulacao && simulacao.tipo === 'critico';
        const alertaReal = totalAtivos > 0;
        board.classList.toggle('has-real-alert', alertaReal);
        board.classList.toggle('is-sim-critical', Boolean(simCritica));
        if (banner) banner.hidden = !(simCritica || alertaReal);

        if (simCritica) {
            bannerTitle.textContent = 'SIMULAÇÃO — suspeita de vazamento';
            bannerDescription.textContent = simulacao.nome
                + ': cenário visual de demonstração. Nenhum vazamento foi confirmado e nenhum dado foi alterado.';
            bannerTag.textContent = 'SIMULADO';
        } else if (alertaReal) {
            bannerTitle.textContent = 'ATENÇÃO — ' + totalAtivos
                + (totalAtivos === 1 ? ' alerta operacional ativo' : ' alertas operacionais ativos');
            bannerDescription.textContent =
                'Há anormalidades registradas que exigem verificação. Não representam confirmação de vazamento de NaOH.';
            bannerTag.textContent = 'REGISTRADO';
        }

        if (overviewCount) overviewCount.textContent = String(totalAtivos);
        if (overviewTitle) overviewTitle.textContent = alertaReal
            ? (totalAtivos === 1 ? 'Alerta ativo registrado' : 'Alertas ativos registrados')
            : 'Nenhum alerta ativo registrado';
    }

    function restaurarSimulacao() {
        if (!simulacao) return;
        flangeCards.filter(function (item) { return item.dataset.flangeId === simulacao.id; })
            .forEach(function (card) {
                card.classList.remove('is-sim-critical', 'is-sim-warning');
                const pill = card.querySelector('.fc-pill');
                const dadoOriginal = original.get(pill);
                if (pill && dadoOriginal) {
                    pill.textContent = dadoOriginal.texto;
                    pill.className = dadoOriginal.classes;
                }
            });
        mapFlanges.filter(function (item) { return item.dataset.flangeId === simulacao.id; })
            .forEach(function (item) {
                item.classList.remove('is-sim-critical', 'is-sim-warning');
                const label = item.querySelector('.plant-node-status');
                const anterior = original.get(label);
                if (label && anterior) label.textContent = anterior.texto;
            });
        simulacao = null;
        if (simSummary) simSummary.hidden = true;
        if (demoEvent) demoEvent.hidden = true;
        atualizarBanner();
    }

    function iniciarSimulacao() {
        if (!flangeSelect || !typeSelect) return;
        const id = flangeSelect.value;
        const card = flangeCards.find(function (item) { return item.dataset.flangeId === id; });
        if (!card) return;

        restaurarSimulacao();
        const nome = card.querySelector('.fc-code')?.textContent?.trim() || 'Flange selecionada';
        const tipo = typeSelect.value === 'critico' ? 'critico' : 'atencao';
        simulacao = {id:id, nome:nome, tipo:tipo};
        const classe = tipo === 'critico' ? 'is-sim-critical' : 'is-sim-warning';
        const texto = tipo === 'critico' ? 'SIMULAÇÃO · CRÍTICO' : 'SIMULAÇÃO · ATENÇÃO';
        const classePill = tipo === 'critico' ? 'is-simulated-critical' : 'is-simulated-warning';

        flangeCards.filter(function (item) { return item.dataset.flangeId === id; })
            .forEach(function (item) {
                item.classList.add(classe);
                const pill = item.querySelector('.fc-pill');
                if (pill) {
                    original.set(pill, {texto:pill.textContent, classes:pill.className});
                    pill.classList.add(classePill);
                    pill.textContent = texto;
                }
            });

        mapFlanges.filter(function (item) { return item.dataset.flangeId === id; })
            .forEach(function (item) {
                item.classList.add(classe);
                const label = item.querySelector('.plant-node-status');
                if (label) {
                    original.set(label, {texto:label.textContent});
                    label.textContent = tipo === 'critico' ? 'SIMULAÇÃO CRÍTICA' : 'SIMULAÇÃO ATENÇÃO';
                }
            });

        if (simSummary) simSummary.hidden = false;
        if (simTarget) simTarget.textContent = nome;
        if (simScenario) simScenario.textContent = tipo === 'critico'
            ? 'Suspeita fictícia de vazamento: destaque vermelho pulsante'
            : 'Anormalidade fictícia em observação: destaque amarelo';
        if (demoEvent) demoEvent.hidden = false;
        if (demoDescription) demoDescription.textContent = nome + ' · '
            + (tipo === 'critico' ? 'Suspeita de vazamento' : 'Anormalidade em observação')
            + ' · Treinamento visual. Não é um alerta real.';
        atualizarBanner();
        if (modal && typeof modal.close === 'function') modal.close();
        if (simSummary && !simSummary.hidden) {
            document.getElementById('encerrarSimulacao')?.focus();
        }
    }

    async function consultarAlertas() {
        if (consultando || document.hidden) return;
        consultando = true;
        try {
            const resposta = await fetch('/api/monitoramento/eventos', {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: {'Accept':'application/json'}
            });
            if (!resposta.ok) throw new Error('Monitoramento indisponível');
            const dados = await resposta.json();
            totalAtivos = Number(dados.totalAlertasAtivos) || 0;
            atualizarEventos(dados.eventos);
            atualizarFlangesReais(dados.idsFlangesEmAlerta);
            board.dataset.alertasReais = String(totalAtivos);
            atualizarBanner();
            board.dataset.conexao = 'ok';
        } catch (erro) {
            board.dataset.conexao = 'falha';
            // Dados originais permanecem visíveis e não são falsamente zerados.
        } finally {
            consultando = false;
        }
    }

    if (openButton && modal) {
        openButton.addEventListener('click', function () {
            if (typeof modal.showModal === 'function') modal.showModal();
        });
        document.getElementById('fecharSimulacao')?.addEventListener('click', function () { modal.close(); });
        document.getElementById('cancelarSimulacao')?.addEventListener('click', function () { modal.close(); });
        document.getElementById('iniciarSimulacao')?.addEventListener('click', iniciarSimulacao);
        modal.addEventListener('click', function (event) {
            if (event.target === modal) modal.close();
        });
        document.getElementById('encerrarSimulacao')?.addEventListener('click', restaurarSimulacao);
    }

    atualizarBanner();
    consultarAlertas();
    window.setInterval(consultarAlertas, 7000);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) consultarAlertas();
    });
})();
