package com.vexsento.jarwis;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WebViewPagePatchTest {
    @Test
    public void mobileComposerFallbackRemovesNativeDisableAndHandlesTouch() {
        String script = WebViewPagePatch.mobileComposerFallback();

        assertTrue(script.contains("removeAttribute('disabled')"));
        assertTrue(script.contains("document.addEventListener('touchstart'"));
        assertTrue(script.contains("getBoundingClientRect()"));
        assertTrue(script.contains("window.jarwisSubmitComposer"));
        assertTrue(script.contains("window.__jarwisNativeSubmitAt"));
        assertTrue(script.contains("form.requestSubmit()"));
        assertTrue(script.contains("passive:false"));
    }

    @Test
    public void screenPointFallbackCallsInjectedNativeSubmitter() {
        String script = WebViewPagePatch.submitAtScreenPoint(101.5f, 202.25f);

        assertTrue(script.contains("window.__jarwisNativeSubmitAt(101.5,202.25)"));
    }
}
