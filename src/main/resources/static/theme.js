(function () {
'use strict';
const key='flanges-theme';
let saved;
try { saved=localStorage.getItem(key); } catch(e) {}
const theme=(saved==='light'||saved==='dark')?saved:'dark';
document.documentElement.dataset.theme=theme;
document.documentElement.style.colorScheme=theme;
function sync(){
  const dark=document.documentElement.dataset.theme==='dark';
  document.querySelectorAll('.theme-toggle').forEach(b=>{
    b.setAttribute('aria-label',dark?'Ativar modo claro':'Ativar modo escuro');
    b.title=dark?'Ativar modo claro':'Ativar modo escuro';
    b.setAttribute('aria-pressed',String(dark));
    b.textContent=dark?'☀ Modo claro':'☾ Modo escuro';
  });
}
function toggle(){
  const next=document.documentElement.dataset.theme==='dark'?'light':'dark';
  document.documentElement.dataset.theme=next;
  document.documentElement.style.colorScheme=next;
  try { localStorage.setItem(key,next); } catch(e) {}
  sync();
  document.dispatchEvent(new CustomEvent('themechange',{detail:{theme:next}}));
}
function button(){
  const b=document.createElement('button');
  b.type='button'; b.className='theme-toggle';
  b.addEventListener('click',toggle);
  return b;
}
function init(){
  const desktop=document.querySelector('.nav-account');
  const mobile=document.querySelector('.mobile-account-actions');
  if(desktop) desktop.prepend(button());
  if(mobile) mobile.prepend(button());
  if(!desktop&&!mobile){
    const b=button(); b.classList.add('theme-toggle-floating'); document.body.appendChild(b);
  }
  sync();
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);
else init();
})();
