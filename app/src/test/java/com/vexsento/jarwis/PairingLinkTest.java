package com.vexsento.jarwis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class PairingLinkTest {
    @Test
    public void parsesCustomSchemePairingLink() {
        PairingLink link = PairingLink.parse(
                "jarwis://connect?url=http%3A%2F%2F192.168.1.20%3A8876%2F&code=12345678"
        );

        assertEquals("http://192.168.1.20:8876/", link.serverUrl());
        assertEquals("12345678", link.pairingCode());
    }

    @Test
    public void parsesHttpsBridgeFragmentWithoutSendingSecretsToServer() {
        PairingLink link = PairingLink.parse(
                "https://aslan171.github.io/jarwis-mobile/connect.html"
                        + "#url=http%3A%2F%2F10.0.0.8%3A8876%2F&code=87654321"
        );

        assertEquals("http://10.0.0.8:8876/", link.serverUrl());
        assertEquals("87654321", link.pairingCode());
    }

    @Test
    public void rejectsMissingCodeAndPublicServer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PairingLink.parse("jarwis://connect?url=http%3A%2F%2F192.168.1.20%3A8876%2F")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PairingLink.parse(
                        "jarwis://connect?url=http%3A%2F%2F8.8.8.8%3A8876%2F&code=12345678"
                )
        );
    }

    @Test
    public void rejectsUntrustedBridgeHost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PairingLink.parse(
                        "https://example.com/jarwis-mobile/connect.html"
                                + "#url=http%3A%2F%2F192.168.1.20%3A8876%2F&code=12345678"
                )
        );
    }
}
