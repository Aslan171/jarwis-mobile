package com.vexsento.jarwis;

final class WebViewPagePatch {
    private WebViewPagePatch() {
    }

    static String mobileComposerFallback() {
        return "(function(){"
                + "var form=document.getElementById('composer-form');"
                + "var button=document.getElementById('send-button');"
                + "var input=document.getElementById('message-input');"
                + "if(!form||!button||!input||button.dataset.jarwisNativeSend==='1')return false;"
                + "button.removeAttribute('disabled');"
                + "button.style.pointerEvents='auto';"
                + "button.style.touchAction='manipulation';"
                + "button.dataset.jarwisNativeSend='1';"
                + "var lastSubmitAt=0;"
                + "function publishHitBox(){"
                + "var r=button.getBoundingClientRect();"
                + "var vw=window.innerWidth||document.documentElement.clientWidth||1;"
                + "var vh=window.innerHeight||document.documentElement.clientHeight||1;"
                + "var style=window.getComputedStyle(button);"
                + "var visible=r.width>0&&r.height>0&&style.display!=='none'&&style.visibility!=='hidden';"
                + "var active=visible&&!!input.value.trim();"
                + "if(window.JarwisComposerBridge&&typeof window.JarwisComposerBridge.updateSendHitBox==='function')"
                + "window.JarwisComposerBridge.updateSendHitBox(r.left/vw,r.top/vh,r.width/vw,r.height/vh,active);"
                + "}"
                + "function submit(event){"
                + "if(!input.value.trim()||Date.now()-lastSubmitAt<700)return false;"
                + "lastSubmitAt=Date.now();"
                + "if(event&&event.cancelable)event.preventDefault();"
                + "if(event&&event.stopImmediatePropagation)event.stopImmediatePropagation();"
                + "if(typeof window.jarwisSubmitComposer==='function')window.jarwisSubmitComposer();"
                + "else if(typeof window.sendMessage==='function')window.sendMessage();"
                + "else if(typeof form.requestSubmit==='function')form.requestSubmit();"
                + "else form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));"
                + "setTimeout(publishHitBox,0);"
                + "return true;"
                + "}"
                + "function hit(x,y){var r=button.getBoundingClientRect(),m=12;"
                + "return x>=r.left-m&&x<=r.right+m&&y>=r.top-m&&y<=r.bottom+m;}"
                + "function point(event){return event.touches&&event.touches[0]||"
                + "event.changedTouches&&event.changedTouches[0]||event;}"
                + "function capture(event){var p=point(event);if(p&&hit(p.clientX,p.clientY))submit(event);}"
                + "document.addEventListener('touchstart',capture,{capture:true,passive:false});"
                + "button.addEventListener('click',submit,true);"
                + "input.addEventListener('input',publishHitBox);"
                + "window.addEventListener('resize',publishHitBox,{passive:true});"
                + "document.addEventListener('click',function(){setTimeout(publishHitBox,0);},true);"
                + "if(typeof ResizeObserver==='function')new ResizeObserver(publishHitBox).observe(button);"
                + "window.jarwisRefreshNativeComposerHitBox=publishHitBox;"
                + "window.__jarwisNativeSubmitAt=function(rawX,rawY){"
                + "var ratio=window.devicePixelRatio||1;"
                + "return (hit(rawX,rawY)||hit(rawX/ratio,rawY/ratio))?submit(null):false;"
                + "};"
                + "setTimeout(publishHitBox,0);"
                + "return true;"
                + "})()";
    }

    static String submitDirectly() {
        return "(function(){try{"
                + "var input=document.getElementById('message-input');"
                + "var form=document.getElementById('composer-form');"
                + "if(!input||!form)return 'missing';"
                + "if(!input.value.trim()){input.focus();return 'empty';}"
                + "if(typeof window.jarwisSubmitComposer==='function'){window.jarwisSubmitComposer();return 'submitted';}"
                + "if(typeof form.requestSubmit==='function'){form.requestSubmit();return 'submitted';}"
                + "form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));return 'submitted';"
                + "}catch(error){return 'error';}})()";
    }
}
