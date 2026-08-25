package com.vexsento.jarwis;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WebViewPagePatchTest {
    @Test
    public void mobileComposerFallbackRemovesNativeDisableAndHandlesTouch() {
        String script = WebViewPagePatch.mobileComposerFallback();

        assertTrue(script.contains("removeAttribute('disabled')"));
        assertTrue(script.contains("addEventListener('touchend'"));
        assertTrue(script.contains("form.requestSubmit()"));
        assertTrue(script.contains("{passive:false}"));
    }
}
