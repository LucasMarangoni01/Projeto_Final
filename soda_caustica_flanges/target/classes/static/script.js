
            const S = { leaks:{1:false,2:false,3:false,4:false,5:false,6:false}, evts:0 };
            const N = { 1:'F-01 (A1)',2:'F-02 (A2)',3:'F-03 (B1)',4:'F-04 (B2)',5:'F-05 (C1)',6:'F-06 (C2)' };
            function pad(n){return String(n).padStart(2,'0');}
            function now(){const d=new Date();return pad(d.getHours())+':'+pad(d.getMinutes())+':'+pad(d.getSeconds());}
            document.getElementById('t0').textContent=now();
            function addLog(msg,al){
              const b=document.getElementById('log');
              const e=document.createElement('div'); e.className='log-row';
              e.innerHTML=`<span class="lt">${now()}</span><span class="${al?'la':''}">${msg}</span>`;
              b.insertBefore(e,b.firstChild);
              if(b.children.length>25)b.removeChild(b.lastChild);
            }
            function updateUI(){
              const lk=Object.values(S.leaks).filter(Boolean).length;
              document.getElementById('ok-c').textContent=6-lk;
              document.getElementById('lk-c').textContent=lk;
              document.getElementById('ev-c').textContent=S.evts;
              const badge=document.getElementById('badge');
              if(lk>0){badge.textContent='ALERTA DE VAZAMENTO';badge.className='badge al-b';}
              else{badge.textContent='SISTEMA NORMAL';badge.className='badge ok-b';}
            }
            function setLeak(id,active){
              S.leaks[id]=active;
              const led=document.getElementById('led'+id);
              const drop=document.getElementById('drop'+id);
              if(active){
                led.setAttribute('fill','#e24b4a');
                led.setAttribute('stroke','#a32d2d');
                led.classList.add('blink');
                drop.setAttribute('opacity','1');
                S.evts++;
                document.getElementById('ts-v').textContent=now().substring(0,5);
                addLog('VAZAMENTO detectado — flange '+N[id],true);
              } else {
                led.setAttribute('fill','#27a060');
                led.setAttribute('stroke','#1d7a4a');
                led.classList.remove('blink');
                drop.setAttribute('opacity','0');
                addLog('Flange '+N[id]+' normalizada pelo operador.',false);
              }
              updateUI();
            }
            function toggleLeak(id){setLeak(id,!S.leaks[id]);}
            function simLeak(){const id=Math.ceil(Math.random()*6);setLeak(id,true);}
            function resetAll(){
              [1,2,3,4,5,6].forEach(id=>{if(S.leaks[id])setLeak(id,false);});
              addLog('Reset geral executado pelo operador.',false);
              updateUI();
            }