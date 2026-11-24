/* FlangeGuard — central de alarmes do protótipo. Áudio exclusivamente local. */
(function () {
    'use strict';

    function pendentes(painel) {
        return [...painel.ativos, ...painel.testesAtivos].filter(a => a.ativo && a.sonoro);
    }
    function escolherAlarme(alarmes, silenciados) {
        const peso = {CRITICA:3, ALTA:2, MODERADA:1};
        return alarmes.filter(a => !silenciados.has(a.chave)).sort((a,b) =>
            Number(a.teste) - Number(b.teste) ||
            (peso[b.gravidade] || 0) - (peso[a.gravidade] || 0) ||
            a.chave.localeCompare(b.chave))[0] || null;
    }
    function padrao(tipo) {
        if (tipo === 'FALHA_COMUNICACAO') return [440, 440];
        if (tipo === 'VAZAMENTO' || tipo === 'VAZAMENTO_CONFIRMADO' || tipo === 'SUSPEITA_VAZAMENTO') return [740, 520, 740];
        return [600, 850];
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = {pendentes, escolherAlarme, padrao};
    if (typeof document === 'undefined') return;
    const $ = id => document.getElementById(id);
    if (!$('centralAlarmes')) return;
    let painel = {ativos:[], recentes:[], testesAtivos:[], testesHistorico:[]};
    let carregado = false, consultando = false, detalhe = null, enviando = false;
    let contexto = null, somAtivo = false, ultimaChave = null;
    let silenciados = new Set(), anterior = new Set(), nosAudio = [];
    const tipoTexto = {
        VAZAMENTO:'Vazamento fictício', VAZAMENTO_CONFIRMADO:'Vazamento confirmado no registro',
        SUSPEITA_VAZAMENTO:'Possível vazamento — não confirmado',
        CRITICO:'Condição crítica registrada', FALHA_COMUNICACAO:'Falha de sensor/comunicação registrada'
    };
    function descricao(a) {
        if (a.teste && a.tipo === 'CRITICO') return 'Condição crítica fictícia';
        if (a.teste && a.tipo === 'FALHA_COMUNICACAO') return 'Falha de comunicação fictícia';
        return tipoTexto[a.tipo] || 'Ocorrência em verificação';
    }
    function criar(tag, texto, classe) {
        const e = document.createElement(tag);
        if (texto !== undefined) e.textContent = texto;
        if (classe) e.className = classe;
        return e;
    }
    function hora(valor) {
        if (!valor) return '—';
        const d = new Date(valor);
        return Number.isNaN(d.getTime()) ? valor : d.toLocaleString('pt-BR');
    }
    function pararAudio() {
        for (const n of nosAudio) {
            try {n.stop();} catch (_) {}
            try {n.disconnect();} catch (_) {}
        }
        nosAudio = [];
        ultimaChave = null;
    }
    function tocar() {
        const alvo = escolherAlarme(pendentes(painel), silenciados);
        if (!somAtivo || !contexto || contexto.state !== 'running' || !alvo || document.hidden) {
            pararAudio();
            return;
        }
        if (ultimaChave !== alvo.chave) pararAudio();
        // Uma única sequência por vez; ganho baixo e envelopes suaves.
        const inicio = contexto.currentTime + .025;
        const frequencias = padrao(alvo.tipo);
        ultimaChave = alvo.chave;
        nosAudio = nosAudio.filter(n => n.fim > contexto.currentTime);
        if (nosAudio.length) return;
        frequencias.forEach((freq,i) => {
            const osc = contexto.createOscillator(), ganho = contexto.createGain();
            const t = inicio + i * .29;
            osc.type = 'sine';
            osc.frequency.value = freq;
            ganho.gain.setValueAtTime(0,t);
            ganho.gain.linearRampToValueAtTime(.035,t+.025);
            ganho.gain.setValueAtTime(.035,t+.15);
            ganho.gain.linearRampToValueAtTime(0,t+.2);
            osc.connect(ganho); ganho.connect(contexto.destination);
            osc.fim = t+.22;
            osc.onended = () => {osc.disconnect(); ganho.disconnect();};
            osc.start(t); osc.stop(t+.22);
            nosAudio.push(osc);
        });
    }
    function atualizarSom() {
        const p = pendentes(painel);
        const alvo = escolherAlarme(p, silenciados);
        let texto = !somAtivo ? 'Desativado — ative o som' :
            contexto?.state !== 'running' ? 'Som bloqueado/pausado' :
            !p.length ? 'Ativado · sem alarme pendente' :
            !alvo ? 'Silenciado nesta sessão' :
            document.hidden ? 'Aba em segundo plano' : 'Alarme ' + (alvo.teste ? 'DE TESTE' : 'REAL');
        $('caSom').textContent = texto;
        $('caSom').dataset.silenciado = String(somAtivo && p.length > 0 && !alvo);
        $('caSilenciar').disabled = !somAtivo || !alvo;
        $('caRetomar').disabled = !somAtivo || !p.some(a => silenciados.has(a.chave));
        if (!alvo) pararAudio();
        else if (ultimaChave && ultimaChave !== alvo.chave) pararAudio();
    }
    $('caAtivarSom').addEventListener('click', async () => {
        try {
            const Audio = window.AudioContext || window.webkitAudioContext;
            if (!Audio) throw new Error('Áudio não disponível');
            if (!contexto) contexto = new Audio();
            await contexto.resume();
            somAtivo = contexto.state === 'running';
            $('caAtivarSom').textContent = somAtivo ? 'Som ativado' : 'Ativar som';
            atualizarSom(); tocar();
        } catch (_) { $('caSom').textContent = 'Áudio indisponível neste navegador'; }
    });
    $('caSilenciar').addEventListener('click', () => {
        pendentes(painel).forEach(a => silenciados.add(a.chave));
        pararAudio(); atualizarSom();
    });
    $('caRetomar').addEventListener('click', () => {
        silenciados.clear(); atualizarSom(); tocar();
    });

    function listar(id, items, vazio) {
        const host = $(id);
        if (!host) return;
        const fragmento = document.createDocumentFragment();
        if (!items.length) fragmento.append(criar('p', vazio, 'ca-empty'));
        for (const a of items) {
            const b = criar('button', undefined, 'ca-event');
            b.type = 'button';
            b.dataset.teste = String(a.teste);
            b.dataset.ativo = String(a.ativo);
            b.dataset.tipo = a.tipo;
            const esquerda = criar('span',undefined,'ca-event-main');
            esquerda.append(criar('strong',(a.teste ? 'TESTE · ' : 'REAL · ') + a.flange),
                criar('span',descricao(a)),criar('small',a.localizacao+' · '+hora(a.detectadoEm)));
            const direita = criar('span',undefined,'ca-event-state');
            direita.append(criar('span',a.gravidade),criar('small',a.rotuloEstado));
            b.append(esquerda,direita);
            b.addEventListener('click',()=>abrirDetalhe(a.chave));
            fragmento.append(b);
        }
        host.replaceChildren(fragmento);
    }
    function todos() {return [...painel.ativos,...painel.recentes,...painel.testesAtivos,...painel.testesHistorico];}
    function abrirDetalhe(chave) {
        detalhe = chave;
        renderDetalhe();
        if (!$('caDetalhes').open) $('caDetalhes').showModal();
    }
    function acaoBotao(label, fn) {
        const b = criar('button',label);
        b.type = 'button'; b.addEventListener('click',fn);
        return b;
    }
    function renderDetalhe() {
        const a = todos().find(x => x.chave === detalhe);
        const host = $('caDetalheConteudo'), acoes = $('caDetalheAcoes');
        host.replaceChildren(); acoes.replaceChildren();
        if (!a) { host.append(criar('p','Registro indisponível no resumo atual.')); return; }
        $('caDetalheTitulo').textContent = (a.teste ? 'TESTE · ' : 'OCORRÊNCIA REAL · ') + a.flange;
        const dl = criar('dl',undefined,'ca-details');
        const linhas = [['Tipo',descricao(a)],['Gravidade',a.gravidade],
            ['Situação',a.rotuloEstado],['Localização',a.localizacao],['Detectado em',hora(a.detectadoEm)]];
        if (a.iniciadoPor) linhas.push(['Iniciado por',a.iniciadoPor]);
        if (a.reconhecidoPor) linhas.push(['Reconhecido por',a.reconhecidoPor],['Reconhecido em',hora(a.reconhecidoEm)]);
        if (a.atendimentoPor) linhas.push(['Atendimento por',a.atendimentoPor],['Atendimento em',hora(a.atendimentoEm)]);
        if (a.encerradoPor) linhas.push(['Encerrado por',a.encerradoPor],['Encerrado em',hora(a.encerradoEm)]);
        for (const [k,v] of linhas) dl.append(criar('dt',k),criar('dd',v || '—'));
        host.append(dl);
        if (a.ativo && a.teste) {
            if (a.sonoro) acoes.append(acaoBotao('Reconhecer teste',()=>agir(a,'reconhecer')));
            if (a.estado === 'RECONHECIDO') acoes.append(acaoBotao('Iniciar atendimento',()=>agir(a,'atender')));
            acoes.append(acaoBotao('Encerrar teste',()=> {
                if (window.confirm('Encerrar apenas este exercício? Ocorrências reais não serão modificadas.')) agir(a,'encerrar');
            }));
        } else if (a.ativo) {
            if (a.sonoro) {
                const label = criar('label','Justificativa do reconhecimento');
                label.htmlFor = 'caJustificativa';
                const campo = criar('textarea');
                campo.id = 'caJustificativa'; campo.maxLength = 500; campo.rows = 3;
                host.append(label,campo);
                acoes.append(acaoBotao('Reconhecer ocorrência',()=>{
                    if (!campo.value.trim()) { $('caDetalheErro').textContent='Informe a justificativa.'; return; }
                    agir(a,'reconhecer',{justificativa:campo.value});
                }));
            }
            const link = criar('a','Consultar ou normalizar em Alertas');
            link.href='/alertas'; acoes.append(link);
            if (a.estado === 'RECONHECIDO') acoes.append(acaoBotao('Registrar atendimento',()=>agir(a,'atender')));
        }
    }
    $('caFecharDetalhes').addEventListener('click',()=>$('caDetalhes').close());
    $('caDetalhes').addEventListener('close',()=>{detalhe=null; $('caDetalheErro').textContent='';});
    async function post(url, body) {
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        if (!header || !token) throw new Error('Sessão sem proteção CSRF disponível. Recarregue a página.');
        const resposta = await fetch(url,{method:'POST', credentials:'same-origin', redirect:'error',
            headers:{'Content-Type':'application/json',Accept:'application/json',[header]:token},
            body:JSON.stringify(body || {})});
        if (!resposta.ok) throw new Error('Ação não concluída (HTTP '+resposta.status+'). Confira a sessão e o estado atual.');
    }
    async function agir(a, acao, body) {
        if (enviando) return;
        enviando=true;
        $('caDetalheErro').textContent='Enviando…';
        try {
            const raiz = a.teste ? '/api/monitoramento/testes/' : '/api/monitoramento/alarmes/';
            await post(raiz+encodeURIComponent(a.id)+'/'+acao,body);
            $('caDetalheErro').textContent='Ação registrada.';
            await consultar();
        } catch(e) {$('caDetalheErro').textContent=e.message;}
        finally {enviando=false;}
    }
    const modal = $('caModalTeste');
    if (modal) {
        $('caAbrirTeste').addEventListener('click',()=>{ $('caFormTeste').reset(); $('caTesteErro').textContent=''; modal.showModal();});
        $('caCancelar').addEventListener('click',()=>modal.close());
        $('caFormTeste').addEventListener('submit',async e=>{
            e.preventDefault(); if(enviando) return;
            enviando=true; $('caIniciar').disabled=true;
            try {
                await post('/api/monitoramento/testes',{flangeId:Number($('caFlange').value),
                    tipo:$('caTipo').value, gravidade:$('caGravidade').value, confirmado:$('caConfirmar').checked});
                modal.close(); await consultar();
            } catch(err) {$('caTesteErro').textContent=err.message;}
            finally {enviando=false; $('caIniciar').disabled=false;}
        });
    }
    function grafico(id, eventos) {
        const host = $(id); if (!host) return;
        const grupos = [
            ['Vazamento / suspeita', eventos.filter(a=>['VAZAMENTO','SUSPEITA_VAZAMENTO','VAZAMENTO_CONFIRMADO'].includes(a.tipo)).length],
            ['Condição crítica', eventos.filter(a=>a.tipo==='CRITICO').length],
            ['Comunicação', eventos.filter(a=>a.tipo==='FALHA_COMUNICACAO').length]
        ];
        const fragmento = document.createDocumentFragment();
        for (const [rotulo, qtd] of grupos) {
            const row = criar('div',undefined,'ca-chart-row');
            const label = criar('span',rotulo);
            const meter = criar('meter'); meter.min=0; meter.max=Math.max(1,eventos.length); meter.value=qtd;
            meter.setAttribute('aria-label',rotulo+': '+qtd);
            row.append(label,meter,criar('strong',String(qtd)));
            fragmento.append(row);
        }
        host.replaceChildren(fragmento);
    }
    function render() {
        grafico('caGraficoReais',painel.ativos);
        grafico('caGraficoTestes',painel.testesAtivos);
        $('caReais').textContent=String(painel.ativos.length);
        $('caPendentes').textContent=String(painel.ativos.filter(a=>a.sonoro).length);
        $('caTestes').textContent=String(painel.testesAtivos.length);
        listar('caAtivos',painel.ativos,'Nenhuma ocorrência real ativa registrada.');
        listar('caHistorico',painel.recentes,'Nenhum registro recente.');
        listar('caTestesAtivos',painel.testesAtivos,'Nenhum teste em andamento.');
        listar('caTestesHistorico',painel.testesHistorico,'Nenhum teste encerrado.');
        if(detalhe && $('caDetalhes').open && document.activeElement?.id !== 'caJustificativa') renderDetalhe();
        atualizarSom();
    }
    async function consultar() {
        if (consultando || document.hidden) return;
        consultando=true;
        try {
            const res=await fetch('/api/monitoramento/alarmes',{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
            if(!res.ok) throw new Error('Consulta indisponível');
            const novo=await res.json();
            if(!['ativos','recentes','testesAtivos','testesHistorico'].every(k=>Array.isArray(novo[k]))) throw new Error('Dados inválidos');
            const novos=pendentes(novo), chaves=new Set(novos.map(a=>a.chave));
            const chegadas=novos.filter(a=>!anterior.has(a.chave));
            silenciados=new Set([...silenciados].filter(k=>chaves.has(k)));
            if(chegadas.length) {
                const real=chegadas.filter(a=>!a.teste);
                const selecionado=real[0] || chegadas[0];
                $('caNotificacao').textContent=(selecionado.teste?'TESTE · ':'NOVA OCORRÊNCIA REAL · ')+selecionado.flange+' · '+descricao(selecionado);
                $('caNotificacao').dataset.teste=String(selecionado.teste);
                $('caNotificacao').dataset.chave=selecionado.chave;
            } else if(!chaves.size) $('caNotificacao').textContent='';
            else if (!chaves.has($('caNotificacao').dataset.chave)) {
                const a=novos.find(x=>!x.teste)||novos[0];
                $('caNotificacao').textContent=(a.teste?'TESTE · ':'OCORRÊNCIA REAL PENDENTE · ')+a.flange+' · '+descricao(a);
                $('caNotificacao').dataset.teste=String(a.teste);
                $('caNotificacao').dataset.chave=a.chave;
            }
            const mudou=!carregado || JSON.stringify(novo)!==JSON.stringify(painel);
            painel=novo; anterior=chaves; carregado=true;
            $('caConexao').textContent='Consulta atualizada · '+new Date().toLocaleTimeString('pt-BR');
            if(mudou) {
                render();
                document.dispatchEvent(new CustomEvent('flangeguard:testes-atualizados',
                    {detail:{testes:painel.testesAtivos, reais:painel.ativos}}));
            } else atualizarSom();
            tocar();
        } catch(_) {
            $('caConexao').textContent=carregado?'Consulta indisponível · exibindo último registro':'Consulta indisponível · estado desconhecido';
        } finally {consultando=false;}
    }
    window.setInterval(()=>{atualizarSom(); tocar();},2200);
    window.setInterval(consultar,7000);
    document.addEventListener('visibilitychange',()=>{if(document.hidden) pararAudio(); else {consultar(); atualizarSom();}});
    window.addEventListener('pagehide',pararAudio);
    consultar();
})();
