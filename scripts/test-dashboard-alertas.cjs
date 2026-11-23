'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const app = require('../src/main/resources/static/dashboard-simulacao.js');
const root = path.resolve(__dirname, '..');
const html = fs.readFileSync(path.join(root,
    'src/main/resources/templates/home/index.html'), 'utf8');
const css = fs.readFileSync(path.join(root,
    'src/main/resources/static/dashboard-monitoramento.css'), 'utf8');
const script = fs.readFileSync(path.join(root,
    'src/main/resources/static/dashboard-simulacao.js'), 'utf8');

function item(id, codigo, prioridade, ordemOriginal) {
    return {id, ordemOriginal, estado:{codigo, prioridade}};
}
test('um vazamento suspeito fica acima dos normais', () => {
    const original = [item('1','NORMAL',0,0),item('2','SUSPEITA',90,1)];
    assert.deepEqual(app.ordenarPorPrioridade(original).map(x=>x.id), ['2','1']);
});
test('duas flanges criticas sao priorizadas e mantem ordem estavel', () => {
    const original = [
        item('1','NORMAL',0,0),item('2','DANIFICADA',95,1),
        item('3','SUSPEITA',90,2),item('4','DANIFICADA',95,3)
    ];
    assert.deepEqual(app.ordenarPorPrioridade(original).map(x=>x.id),
        ['2','4','3','1']);
});
test('dano sem vazamento e uma condicao independente', () => {
    const dano = app.simularEstado('danificada');
    assert.equal(dano.codigo,'DANIFICADA');
    assert.match(dano.rotulo,/SIMULAÇÃO.*DANIFICADA/);
    assert.ok(dano.prioridade > app.simularEstado('critico').prioridade);
});
test('falha de sensor tem sinal diferente de vazamento', () => {
    const falha = app.simularEstado('falha');
    assert.equal(falha.codigo,'FALHA_SENSOR');
    assert.equal(app.CRITICOS.has(falha.codigo),false);
    assert.equal(app.PROBLEMAS.has(falha.codigo),true);
});
test('simulacao e sinalizada e nao possui transporte de escrita', () => {
    const sim = app.simularEstado('critico');
    assert.match(sim.rotulo,/SIMULAÇÃO/);
    assert.equal(sim.codigo,'SUSPEITA');
    assert.ok(script.includes("fetch('/api/monitoramento/eventos'"));
    assert.ok(!script.includes("method:'POST'"));
    assert.ok(!script.includes("method: 'POST'"));
    assert.ok(!script.includes('fetch("/api/monitoramento/eventos", {method'));
});
test('mapa e cards tem pontos identificaveis e estado renderizado', () => {
    assert.match(html,/class="plant-flange"/);
    assert.match(html,/class="dashboard-flange-card"/);
    assert.match(html,/data-visual-state=\$\{flange.estadoVisual.codigo\}/);
    assert.match(html,/class="plant-alert-wave"/);
    assert.match(html,/class="fc-alert-icon"/);
    assert.match(script,/card.scrollIntoView/);
    assert.match(script,/container.scrollBy/);
    assert.match(script,/role="button"/.test(html) ? /ligarSelecao/ : /^NUNCA$/);
});
test('animacao somente em anel, luz e borda; acessivel sem movimento', () => {
    assert.match(css,/@keyframes pulse-red/);
    assert.match(css,/@keyframes critical-glow/);
    assert.match(css,/@keyframes alert-ring/);
    assert.match(css,/@keyframes critical-border-glow/);
    assert.match(css,/prefers-reduced-motion:reduce/);
    assert.match(css,/\.dashboard-flange-card\.is-selected/);
    assert.match(css,/html\[data-theme="light"\]/);
    assert.match(script,/recebidos\.forEach/);
});
