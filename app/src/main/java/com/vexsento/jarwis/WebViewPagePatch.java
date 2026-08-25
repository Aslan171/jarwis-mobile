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
                + "function submit(event){"
                + "if(!input.value.trim()||Date.now()-lastSubmitAt<700)return false;"
                + "lastSubmitAt=Date.now();"
                + "if(event&&event.cancelable)event.preventDefault();"
                + "if(event&&event.stopImmediatePropagation)event.stopImmediatePropagation();"
                + "if(typeof window.jarwisSubmitComposer==='function')window.jarwisSubmitComposer();"
                + "else if(typeof window.sendMessage==='function')window.sendMessage();"
                + "else if(typeof form.requestSubmit==='function')form.requestSubmit();"
                + "else form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));"
                + "return true;"
                + "}"
                + "function hit(x,y){var r=button.getBoundingClientRect(),m=12;"
                + "return x>=r.left-m&&x<=r.right+m&&y>=r.top-m&&y<=r.bottom+m;}"
                + "function point(event){return event.touches&&event.touches[0]||"
                + "event.changedTouches&&event.changedTouches[0]||event;}"
                + "function capture(event){var p=point(event);if(p&&hit(p.clientX,p.clientY))submit(event);}"
                + "document.addEventListener('touchstart',capture,{capture:true,passive:false});"
                + "button.addEventListener('click',submit,true);"
                + "window.__jarwisNativeSubmitAt=function(rawX,rawY){"
                + "var ratio=window.devicePixelRatio||1;"
                + "return (hit(rawX,rawY)||hit(rawX/ratio,rawY/ratio))?submit(null):false;"
                + "};"
                + "return true;"
                + "})()";
    }

    static String submitAtScreenPoint(float x, float y) {
        return "(function(){return typeof window.__jarwisNativeSubmitAt==='function'"
                + "?window.__jarwisNativeSubmitAt(" + Float.toString(x) + "," + Float.toString(y) + ")"
                + ":false;})()";
    }
}
