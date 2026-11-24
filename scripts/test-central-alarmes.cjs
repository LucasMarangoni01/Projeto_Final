'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {pendentes, escolherAlarme, padrao} = require('../src/main/resources/static/js/central-alarmes.js');
const item = (chave, teste, gravidade, tipo, sonoro=true, ativo=true) => ({chave,teste,gravidade,tipo,sonoro,ativo});
test('sons diferentes para categorias e somente um alarme escolhido',()=>{
    const a=item('real:1',false,'ALTA','CRITICO');
    const b=item('teste:1',true,'CRITICA','VAZAMENTO');
    assert.equal(escolherAlarme([b,a],new Set()),a);
    assert.notDeepEqual(padrao('VAZAMENTO'),padrao('CRITICO'));
    assert.notDeepEqual(padrao('FALHA_COMUNICACAO'),padrao('CRITICO'));
});
test('silenciar registros atuais nao silencia a nova emergência',()=>{
    const a=item('real:1',false,'CRITICA','VAZAMENTO_CONFIRMADO');
    const b=item('real:2',false,'ALTA','FALHA_COMUNICACAO');
    const muted=new Set([a.chave]);
    assert.equal(escolherAlarme([a],muted),null);
    assert.equal(escolherAlarme([a,b],muted),b);
});
test('reconhecimento silencia sem resolver e encerramento de teste nao afeta real',()=>{
    const a=item('real:1',false,'CRITICA','CRITICO');
    const b=item('teste:1',true,'CRITICA','VAZAMENTO',false,false);
    const c=item('real:2',false,'CRITICA','CRITICO',false,true);
    const painel={ativos:[a,c],testesAtivos:[b]};
    assert.deepEqual(pendentes(painel),[a]);
    assert.equal(escolherAlarme(pendentes(painel),new Set()),a);
});
test('controle de acesso no servidor, csrf e sobreposição isolada',()=>{
    const root=path.resolve(__dirname,'..');
    const read=p=>fs.readFileSync(path.join(root,p),'utf8');
    const html=read('src/main/resources/templates/home/index.html');
    const js=read('src/main/resources/static/js/central-alarmes.js');
    const map=read('src/main/resources/static/dashboard-simulacao.js');
    const security=read('src/main/java/com/pbe/soda_caustica_flanges/config/SecurityConfig.java');
    assert.match(html,/th:if="\$\{ehAdmin\}" id="caModalTeste"/);
    assert.match(html,/name="_csrf"/);
    assert.match(js,/\[header\]:token/);
    assert.match(js,/redirect:'error'/);
    assert.match(map,/const estadosReaisCentral = new Map/);
    assert.match(map,/testeEmergencia:true/);
    assert.match(security,/requestMatchers\("\/api\/monitoramento\/testes", "\/api\/monitoramento\/testes\/\*\*"\)\.hasRole\("ADMIN"\)/);
});
