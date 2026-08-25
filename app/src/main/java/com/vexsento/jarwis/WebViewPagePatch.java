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
                + "button.addEventListener('touchend',function(event){"
                + "if(!input.value.trim())return;"
                + "event.preventDefault();"
                + "if(typeof form.requestSubmit==='function')form.requestSubmit();"
                + "else form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));"
                + "},{passive:false});"
                + "return true;"
                + "})()";
    }
}
