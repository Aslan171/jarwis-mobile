package com.vexsento.jarwis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ServerAddressTest {
    @Test
    public void addsSchemePortAndTrailingSlash() {
        assertEquals("http://192.168.1.20:8876/", ServerAddress.normalize("192.168.1.20"));
    }

    @Test
    public void preservesExplicitPrivateAddress() {
        assertEquals("https://10.0.0.5:9443/", ServerAddress.normalize("https://10.0.0.5:9443/path"));
    }

    @Test
    public void rejectsPublicAndMalformedHosts() {
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.normalize("8.8.8.8"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.normalize("jarwis.example.com"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.normalize("192.168.999.1"));
    }

    @Test
    public void recognizesPrivateRanges() {
        assertTrue(ServerAddress.isPrivateIpv4("10.4.5.6"));
        assertTrue(ServerAddress.isPrivateIpv4("172.16.0.1"));
        assertTrue(ServerAddress.isPrivateIpv4("172.31.255.254"));
        assertTrue(ServerAddress.isPrivateIpv4("192.168.50.2"));
        assertFalse(ServerAddress.isPrivateIpv4("172.32.0.1"));
        assertFalse(ServerAddress.isPrivateIpv4("149.40.51.230"));
    }
}
