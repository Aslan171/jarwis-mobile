package com.vexsento.jarwis;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class LocalNetworkPrefixesTest {
    @Test
    public void keepsWifiPrefixBeforeMobileAndVpnPrefixes() {
        List<String> result = LocalNetworkPrefixes.fromHostAddresses(Arrays.asList(
                "192.168.1.77",
                "10.24.18.9",
                "172.20.10.4"
        ));

        assertEquals(Arrays.asList("192.168.1.", "10.24.18.", "172.20.10."), result);
    }

    @Test
    public void removesDuplicatesAndRejectsNonLocalAddresses() {
        List<String> result = LocalNetworkPrefixes.fromHostAddresses(Arrays.asList(
                "192.168.1.77",
                "192.168.1.78",
                "127.0.0.1",
                "8.8.8.8",
                "not-an-address"
        ));

        assertEquals(Collections.singletonList("192.168.1."), result);
    }
}
