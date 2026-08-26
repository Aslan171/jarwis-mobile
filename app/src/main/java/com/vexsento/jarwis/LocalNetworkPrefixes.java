package com.vexsento.jarwis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LocalNetworkPrefixes {
    private LocalNetworkPrefixes() {
    }

    public static List<String> fromHostAddresses(Iterable<String> hostAddresses) {
        Set<String> prefixes = new LinkedHashSet<>();
        for (String host : hostAddresses) {
            if (!ServerAddress.isPrivateIpv4(host) || host.startsWith("127.")) {
                continue;
            }
            int split = host.lastIndexOf('.');
            if (split > 0) {
                prefixes.add(host.substring(0, split + 1));
            }
        }
        return new ArrayList<>(prefixes);
    }
}
