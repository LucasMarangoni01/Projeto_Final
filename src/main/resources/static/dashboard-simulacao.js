/*
 * FlangeGuard · alertas dinamicos e simulacao visual.
 * O servidor e a unica fonte de estados reais. Simulacoes sao locais,
 * jamais criam incidentes, eventos MQTT ou leituras persistidas.
 */
(function () {
    'use strict';

    const CRITICOS = new Set(['VAZAMENTO', 'SUSPEITA', 'DANIFICADA', 'CRITICO']);
    const PROBLEMAS = new Set(['VAZAMENTO', 'SUSPEITA', 'DANIFICADA', 'CRITICO',
        'FALHA_SENSOR', 'LEITURA_DESATUALIZADA', 'ATENCAO']);
    const CODIGOS = new Set([...PROBLEMAS, 'NORMAL', 'MANUTENCAO',
        'SEM_SENSOR', 'NAO_INSTALADO', 'AGUARDANDO_DADOS',
        'LEITURA_DESATUALIZADA']);

    // Logica pura usada tambem nos testes automatizados (Node.js).
    function prioridadeOrdenacao(estado) {
        return Number(estado?.prioridade) || 0;
    }
    function ordenarPorPrioridade(items) {
        return [...items].sort(function (a, b) {
            return prioridadeOrdenacao(b.estado) - prioridadeOrdenacao(a.estado)
                || a.ordemOriginal - b.ordemOriginal;
        });
    }
    function simularEstado(tipo) {
        const estados = {
            atencao: {codigo:'ATENCAO', rotulo:'SIMULAÇÃO · ATENÇÃO',
                mensagem:'Cenário fictício de anormalidade para treinamento.', prioridade:45},
            critico: {codigo:'SUSPEITA', rotulo:'SIMULAÇÃO · SUSPEITA',
                mensagem:'Suspeita fictícia de vazamento, não confirmada.', prioridade:90},
            danificada: {codigo:'DANIFICADA', rotulo:'SIMULAÇÃO · DANIFICADA',
                mensagem:'Dano fictício na flange para treinamento.', prioridade:95},
            falha: {codigo:'FALHA_SENSOR', rotulo:'SIMULAÇÃO · FALHA',
                mensagem:'Falha fictícia de comunicação ou sensor.', prioridade:75}
        };
        return estados[tipo] || estados.atencao;
    }
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = {prioridadeOrdenacao, ordenarPorPrioridade, simularEstado, CRITICOS, PROBLEMAS};
    }
    if (typeof document === 'undefined') return;

    const board = document.getElementById('painel-monitoramento');
    if (!board) return;
    const lista = document.getElementById('dynamic-flange-list');
    const cards = Array.from(document.querySelectorAll('.dashboard-flange-card[data-flange-id]'));
    const mapa = Array.from(document.querySelectorAll('.plant-flange[data-flange-id]'));
    const cardsPorId = new Map(cards.map(el => [el.dataset.flangeId, el]));
    const mapaPorId = new Map(mapa.map(el => [el.dataset.flangeId, el]));
    const busca = document.getElementById('flangeSearch');

    const feed = document.getElementById('monitorEventFeed');
    const count = document.getElementById('monitorActiveCount');
    const countTitle = document.getElementById('monitorOverviewTitle');
    const badgeAlertas = document.getElementById('flangeAlertCount');
    const badgeSimulacao = document.getElementById('flangeSimCount');
    const selectionStatus = document.getElementById('flangeSelectionStatus');
    const banner = document.getElementById('monitorCriticalBanner');
    const bannerTitle = document.getElementById('monitorCriticalTitle');
    const bannerDescription = document.getElementById('monitorCriticalDescription');
    const bannerTag = document.getElementById('monitorCriticalTag');
    const summary = document.getElementById('monitorSimSummary');
    const summaryTarget = document.getElementById('monitorSimTarget');
    const summaryScenario = document.getElementById('monitorSimScenario');
    const demoEvent = document.getElementById('monitorDemoEvent');
    const demoDescription = document.getElementById('monitorDemoDescription');
    const modal = document.getElementById('modalSimulacao');
    const openButton = document.getElementById('abrirSimulacao');
    const flangeSelect = document.getElementById('monitorSimFlange');
    const typeSelect = document.getElementById('monitorSimTipo');
    const feedback = document.getElementById('monitorSimFeedback');
    const connectionLabel = document.getElementById('monitorConnectionStatus');

    const real = new Map();
    cards.forEach(function (card, index) {
        real.set(card.dataset.flangeId, {
            codigo: CODIGOS.has(card.dataset.visualState) ? card.dataset.visualState : 'NORMAL',
            rotulo: card.querySelector('.fc-pill')?.textContent?.trim() || 'NORMAL',
            mensagem: card.querySelector('.fc-condition-message')?.textContent?.trim() || '',
            prioridade: Number(card.dataset.priority) || 0,
            alertaAtivo: card.dataset.alarmActive === 'true'
        });
        card.dataset.originalIndex = String(index);
    });

    let totalReal = Number(board.dataset.alertasReais || 0);
    let simulacao = null; // {id, nome, tipo, estado}; nunca enviado ao servidor
    // Exercícios persistidos e ocorrências reais permanecem em coleções distintas.
    const testesAtivos = new Map();
    let idsReaisCentral = new Set();
    const estadosReaisCentral = new Map();
    let selecionada = null;
    let consultando = false;

    function elemento(tag, classe, valor) {
        const node = document.createElement(tag);
        if (classe) node.className = classe;
        if (valor !== undefined) node.textContent = String(valor);
        return node;
    }
    function estadoEfetivo(id) {
        const atual = real.get(id);
        // Um exercício nunca substitui uma ocorrência real ativa nem um estado crítico real.
        const central = estadosReaisCentral.get(id);
        if (central || atual?.alertaAtivo || (atual?.prioridade || 0) >= 75) {
            if (!central) return atual;
            return atual && atual.prioridade >= central.prioridade && (atual.alertaAtivo || atual.prioridade >= 75) ? atual : central;
        }
        const teste = testesAtivos.get(id);
        if (teste) return teste;
        if (simulacao?.id === id) return {...simulacao.estado, simulado:true};
        return atual || {codigo:'NORMAL', rotulo:'NORMAL',
            mensagem:'Status não disponível', prioridade:0, alertaAtivo:false};
    }
    document.addEventListener('flangeguard:testes-atualizados', function (event) {
        testesAtivos.clear();
        estadosReaisCentral.clear();
        for (const a of event.detail.reais || []) {
            if (a.teste || !a.ativo || a.flangeId == null) continue;
            const id = String(a.flangeId);
            const codigo = {VAZAMENTO_CONFIRMADO:'VAZAMENTO',SUSPEITA_VAZAMENTO:'SUSPEITA',FALHA_COMUNICACAO:'FALHA_SENSOR'}[a.tipo] || 'CRITICO';
            const prioridade = {VAZAMENTO:100,SUSPEITA:90,CRITICO:85,FALHA_SENSOR:75}[codigo];
            const rotulo = {VAZAMENTO:'VAZAMENTO CONFIRMADO',SUSPEITA:'POSSÍVEL VAZAMENTO',CRITICO:'ALERTA CRÍTICO',FALHA_SENSOR:'FALHA DE SENSOR'}[codigo];
            const estado = {codigo,prioridade,rotulo,mensagem:'Ocorrência real registrada · '+a.rotuloEstado+'. Consulte a central.',alertaAtivo:true};
            if (!estadosReaisCentral.has(id) || estadosReaisCentral.get(id).prioridade < prioridade) estadosReaisCentral.set(id,estado);
        }
        idsReaisCentral = new Set(estadosReaisCentral.keys());
        for (const t of event.detail.testes || []) {
            if (!t.teste || !t.ativo) continue;
            const id = String(t.flangeId);
            const falha = t.tipo === 'FALHA_COMUNICACAO';
            const estado = {codigo:falha?'FALHA_SENSOR':t.tipo==='VAZAMENTO'?'SUSPEITA':'CRITICO',
                rotulo:falha?'TESTE · FALHA':t.tipo==='VAZAMENTO'?'TESTE · VAZAMENTO':'TESTE · CRÍTICO',
                mensagem:'Exercício administrativo · '+t.gravidade+' · '+t.rotuloEstado+'. Não é uma ocorrência real.',
                prioridade:(falha?65:80)+({MODERADA:1,ALTA:2,CRITICA:3}[t.gravidade]||0),
                testeEmergencia:true};
            if (!testesAtivos.has(id) || testesAtivos.get(id).prioridade < estado.prioridade) testesAtivos.set(id,estado);
        }
        renderizar();
    });
    function nomeFlange(id) {
        return cardsPorId.get(id)?.querySelector('.fc-code')?.textContent?.trim() || 'Flange';
    }
    function aplicarVisual(id) {
        const estado = estadoEfetivo(id);
        const card = cardsPorId.get(id);
        const group = mapaPorId.get(id);
        if (!card || !group) return;
        const codigo = CODIGOS.has(estado.codigo) ? estado.codigo : 'NORMAL';
        const simulado = Boolean(estado.simulado);
        for (const node of [card, group]) {
            node.dataset.visualState = codigo;
            node.dataset.simulation = String(simulado);
            node.dataset.testeEmergencia = String(Boolean(estado.testeEmergencia));
            node.dataset.priority = String(estado.prioridade || 0);
            node.classList.toggle('is-sim-critical', simulado && CRITICOS.has(codigo));
            node.classList.toggle('is-sim-warning', simulado && !CRITICOS.has(codigo));
            node.classList.toggle('is-selected', selecionada === id);
            node.setAttribute('aria-pressed', String(selecionada === id));
            node.setAttribute('aria-label', 'Localizar ' + nomeFlange(id) + ': ' + estado.rotulo
                + (estado.testeEmergencia ? ', TESTE DE EMERGÊNCIA' : simulado ? ', SIMULAÇÃO' : ', dado registrado'));
        }
        const tituloMapa = group.querySelector('.plant-node-status');
        if (tituloMapa) tituloMapa.textContent = estado.rotulo;
        const pill = card.querySelector('.fc-pill');
        if (pill) pill.textContent = estado.rotulo;
        const mensagem = card.querySelector('.fc-condition-message');
        if (mensagem) mensagem.textContent = estado.mensagem;
        card.dataset.alarmActive = String(Boolean(real.get(id)?.alertaAtivo || idsReaisCentral.has(id)));
        group.dataset.alarmActive = card.dataset.alarmActive;
    }
    function ordenarLista() {
        if (!lista) return;
        const ordenados = ordenarPorPrioridade(cards.map(card => ({
            card,
            estado: estadoEfetivo(card.dataset.flangeId),
            ordemOriginal: Number(card.dataset.originalIndex) || 0
        }))).map(i => i.card);
        const existentes = Array.from(lista.querySelectorAll(':scope > .dashboard-flange-card'));
        if (!ordenados.every((card, idx) => card === existentes[idx])) {
            const scrollTop = lista.scrollTop;
            const vazio = document.getElementById('flangeSearchEmpty');
            ordenados.forEach(card => lista.insertBefore(card, vazio || null));
            lista.scrollTop = scrollTop;
        }
    }
    function atualizarIndicadores() {
        const emAlerta = Array.from(real.values()).filter(item => PROBLEMAS.has(item.codigo)).length;
        if (badgeAlertas) {
            badgeAlertas.textContent = 'Em alerta: ' + emAlerta;
            badgeAlertas.dataset.hasAlert = String(emAlerta > 0);
        }
        if (badgeSimulacao) badgeSimulacao.hidden = !simulacao;
        if (count) count.textContent = String(totalReal);
        if (countTitle) countTitle.textContent = totalReal > 0
            ? (totalReal === 1 ? 'Alerta real ativo registrado' : 'Alertas reais ativos registrados')
            : 'Nenhum alerta ativo registrado';
    }
    function atualizarBanner() {
        const estadosReais = Array.from(real.values());
        const existeReal = totalReal > 0 || estadosReais.some(item => item.alertaAtivo) || estadosReaisCentral.size > 0;
        const criticoReal = [...estadosReais,...estadosReaisCentral.values()].some(item => CRITICOS.has(item.codigo));
        const somenteFalha = existeReal && !criticoReal;
        const simCritica = simulacao && CRITICOS.has(simulacao.estado.codigo);
        const testeCritico = Array.from(testesAtivos.values()).some(t => CRITICOS.has(t.codigo));
        const temTeste = testesAtivos.size > 0;
        if (banner) {
            banner.hidden = !(existeReal || simCritica || temTeste);
            banner.classList.toggle('is-warning', somenteFalha || (!existeReal && temTeste && !testeCritico && !simCritica));
        }
        board.classList.toggle('has-real-alert', existeReal && criticoReal);
        board.classList.toggle('is-sim-critical', Boolean(simCritica || testeCritico) && !existeReal);

        if (existeReal) {
            if (bannerTitle) bannerTitle.textContent = criticoReal
                ? 'ATENÇÃO — ocorrência crítica registrada'
                : 'ATENÇÃO — registro de falha ou anormalidade';
            if (bannerDescription) bannerDescription.textContent = criticoReal
                ? 'Condição registrada que requer avaliação. Sinais automáticos não comprovam vazamento de soda cáustica.'
                : 'Verifique o sensor, a comunicação e a ocorrência; não há confirmação de vazamento.';
            if (bannerTag) bannerTag.textContent = 'DADO REGISTRADO';
        } else if (temTeste) {
            if (bannerTitle) bannerTitle.textContent = 'TESTE DE EMERGÊNCIA — '+testesAtivos.size+' flange(s)';
            if (bannerDescription) bannerDescription.textContent = 'Exercício de software registrado separadamente. Nenhum equipamento foi acionado.';
            if (bannerTag) bannerTag.textContent = 'TESTE';
        } else if (simCritica) {
            if (bannerTitle) bannerTitle.textContent = 'SIMULAÇÃO — ' + simulacao.nome;
            if (bannerDescription) bannerDescription.textContent =
                simulacao.estado.mensagem + ' Nenhum sensor ou registro real foi alterado.';
            if (bannerTag) bannerTag.textContent = 'SIMULADO';
        }
    }
    function renderizar() {
        cards.forEach(card => aplicarVisual(card.dataset.flangeId));
        ordenarLista();
        atualizarIndicadores();
        atualizarBanner();
        if (busca) busca.dispatchEvent(new Event('input', {bubbles:true}));
    }
    function selecionar(id, origem) {
        const card = cardsPorId.get(id);
        const group = mapaPorId.get(id);
        if (!card || !group) return;
        selecionada = id;
        if (card.hidden && busca) {
            busca.value = '';
            busca.dispatchEvent(new Event('input', {bubbles:true}));
        }
        cards.forEach(item => {
            const selecionado = item.dataset.flangeId === id;
            item.classList.toggle('is-selected', selecionado);
            item.setAttribute('aria-pressed', String(selecionado));
        });
        mapa.forEach(item => {
            const selecionado = item.dataset.flangeId === id;
            item.classList.toggle('is-selected', selecionado);
            item.setAttribute('aria-pressed', String(selecionado));
        });
        if (selectionStatus) selectionStatus.textContent =
            nomeFlange(id) + ' · ' + estadoEfetivo(id).rotulo + ' · ponto destacado no mapa.';
        if (origem === 'mapa') {
            card.scrollIntoView({block:'nearest', inline:'nearest', behavior:'smooth'});
            card.focus({preventScroll:true});
        } else {
            const container = document.querySelector('.map-viewport');
            if (container) {
                const area = container.getBoundingClientRect();
                const alvo = group.getBoundingClientRect();
                container.scrollBy({left:(alvo.left + alvo.right - area.left - area.right)/2,
                    behavior:'smooth'});
            }
        }
    }
    function ligarSelecao(node, origem) {
        const id = node.dataset.flangeId;
        node.addEventListener('click', () => selecionar(id, origem));
        node.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                selecionar(id, origem);
            }
        });
    }
    cards.forEach(card => ligarSelecao(card, 'lista'));
    mapa.forEach(node => ligarSelecao(node, 'mapa'));

    function mostrarEventos(eventos) {
        if (!feed || !Array.isArray(eventos)) return;
        const fragmento = document.createDocumentFragment();
        if (!eventos.length) {
            fragmento.append(elemento('p','monitor-empty',
                'Não há ocorrências individuais registradas. Isso não confirma segurança operacional.'));
        } else {
            eventos.slice(0,6).forEach(function (evento) {
                const item = elemento('article','monitor-event '
                    +(evento.ativo ? 'is-active':'is-resolved'));
                const dot = elemento('span','monitor-event-indicator');
                dot.setAttribute('aria-hidden','true');
                const body = elemento('div','monitor-event-body');
                const top = elemento('div','monitor-event-top');
                top.append(
                    elemento('strong','',evento.nomeFlange || 'Flange não vinculada'),
                    elemento('time','',evento.dataHora || '—'));
                body.append(top,
                    elemento('p','',evento.descricao || 'Ocorrência em verificação'),
                    elemento('span','monitor-event-status',
                        evento.situacao || (evento.ativo ? 'Alerta ativo':'Normalizado')));
                item.append(dot,body);
                fragmento.append(item);
            });
        }
        feed.replaceChildren(fragmento);
    }

    function finalizarSimulacao() {
        if (!simulacao) return;
        simulacao = null;
        if (summary) summary.hidden = true;
        if (demoEvent) demoEvent.hidden = true;
        renderizar(); // Retoma exatamente o estado real mais recente.
    }
    function iniciarSimulacao() {
        const id = flangeSelect?.value;
        if (!id || !cardsPorId.has(id)) return;
        const realAtual = real.get(id);
        // Ocorrencia real sempre vence; evita mascarar uma flange ja em alerta.
        if (testesAtivos.has(id) || idsReaisCentral.has(id) || realAtual?.alertaAtivo || (realAtual?.prioridade || 0) >= 75) {
            if (feedback) {
                feedback.hidden = false;
                feedback.textContent = 'Existe uma condição real importante nesta flange. '
                    + 'Escolha outra para não ocultar o alerta registrado.';
            }
            return;
        }
        const tipo = typeSelect?.value || 'atencao';
        const estado = simularEstado(tipo);
        simulacao = {id, tipo, estado, nome:nomeFlange(id)};
        if (summary) summary.hidden = false;
        if (summaryTarget) summaryTarget.textContent = simulacao.nome;
        if (summaryScenario) summaryScenario.textContent = estado.mensagem;
        if (demoEvent) demoEvent.hidden = false;
        if (demoDescription) demoDescription.textContent = simulacao.nome + ' · '
            + estado.rotulo + ' · evento local não registrado no banco.';
        if (modal?.open) modal.close();
        renderizar();
        selecionar(id, 'lista');
        document.getElementById('encerrarSimulacao')?.focus({preventScroll:true});
    }
    if (openButton && modal) {
        openButton.addEventListener('click', function () {
            if (feedback) feedback.hidden = true;
            if (typeof modal.showModal === 'function') modal.showModal();
        });
        document.getElementById('fecharSimulacao')?.addEventListener('click',() => modal.close());
        document.getElementById('cancelarSimulacao')?.addEventListener('click',() => modal.close());
        document.getElementById('iniciarSimulacao')?.addEventListener('click',iniciarSimulacao);
        modal.addEventListener('click', e => {if (e.target === modal) modal.close();});
        document.getElementById('encerrarSimulacao')?.addEventListener('click',finalizarSimulacao);
    }

    async function consultarEstados() {
        if (consultando || document.hidden) return;
        consultando = true;
        try {
            const resposta = await fetch('/api/monitoramento/eventos', {
                credentials: 'same-origin', cache: 'no-store',
                headers:{Accept:'application/json'}
            });
            if (!resposta.ok) throw new Error('Consulta indisponível');
            const dados = await resposta.json();
            if (!Array.isArray(dados.estadosFlanges)) {
                throw new Error('Sem dados de estados das flanges');
            }
            totalReal = Number(dados.totalAlertasAtivos) || 0;
            const recebidos = new Map();
            dados.estadosFlanges.forEach(function (estado) {
                const id = String(estado.flangeId);
                if (!cardsPorId.has(id) || !CODIGOS.has(estado.codigo)) return;
                recebidos.set(id, {
                    codigo:estado.codigo,
                    rotulo:typeof estado.rotulo === 'string' ? estado.rotulo:'VERIFICAR',
                    mensagem:typeof estado.mensagem === 'string' ? estado.mensagem:'',
                    prioridade:Number(estado.prioridade) || 0,
                    alertaAtivo:Boolean(estado.alertaAtivo)
                });
            });
            if (simulacao && recebidos.has(simulacao.id)) {
                const novo = recebidos.get(simulacao.id);
                if (novo.alertaAtivo || novo.prioridade >= 75) finalizarSimulacao();
            }
            // Somente estados das flanges visiveis sao usados; nao inventar
            // flanges novas se o cadastro mudar ate o proximo reload.
            recebidos.forEach((value,id) => real.set(id,value));
            mostrarEventos(dados.eventos);
            board.dataset.alertasReais = String(totalReal);
            board.dataset.conexao = 'ok';
            if (connectionLabel) connectionLabel.textContent = 'Dados atualizados automaticamente.';
            renderizar();
        } catch (error) {
            board.dataset.conexao = 'falha';
            if (connectionLabel) connectionLabel.textContent =
                'Conexão indisponível. Exibindo a última informação recebida; não presuma estado normal.';
        } finally {
            consultando = false;
        }
    }
    renderizar();
    consultarEstados();
    window.setInterval(consultarEstados,7000);
    document.addEventListener('visibilitychange',function () {
        if (!document.hidden) consultarEstados();
    });
})();
