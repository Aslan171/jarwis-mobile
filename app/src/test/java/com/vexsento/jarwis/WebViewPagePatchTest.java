package com.vexsento.jarwis;

import static org.junit.Assert.assertFalse;
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
        assertTrue(script.contains("JarwisComposerBridge.updateSendHitBox"));
        assertTrue(script.contains("new ResizeObserver(publishHitBox)"));
    }

    @Test
    public void nativeOverlaySubmitsWithoutCoordinateDependency() {
        String script = WebViewPagePatch.submitDirectly();

        assertTrue(script.contains("window.jarwisSubmitComposer"));
        assertTrue(script.contains("form.requestSubmit()"));
        assertTrue(script.contains("return 'submitted'"));
        assertTrue(script.contains("return 'missing'"));
    }

    @Test
    public void successfulNativeSubmitKeepsHitTargetUntilWebStateRefreshesIt() {
        assertFalse(MainActivity.nativeSubmitFailed("\"submitted\""));
        assertFalse(MainActivity.nativeSubmitFailed("\"empty\""));
        assertTrue(MainActivity.nativeSubmitFailed("\"missing\""));
        assertTrue(MainActivity.nativeSubmitFailed("\"error\""));
    }
}
