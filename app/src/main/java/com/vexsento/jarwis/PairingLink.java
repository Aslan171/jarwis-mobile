package com.vexsento.jarwis;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PairingLink {
    private static final String BRIDGE_HOST = "aslan171.github.io";
    private static final String BRIDGE_PATH = "/jarwis-mobile/connect.html";

    private final String serverUrl;
    private final String pairingCode;

    private PairingLink(String serverUrl, String pairingCode) {
        this.serverUrl = serverUrl;
        this.pairingCode = pairingCode;
    }

    public static PairingLink parse(String rawValue) {
        final URI uri;
        try {
            uri = new URI(rawValue == null ? "" : rawValue.trim());
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Ссылка подключения записана неверно");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String encodedParameters;
        if (scheme.equals("jarwis") && "connect".equalsIgnoreCase(uri.getHost())) {
            encodedParameters = uri.getRawQuery();
        } else if (
                scheme.equals("https")
                        && BRIDGE_HOST.equalsIgnoreCase(uri.getHost())
                        && BRIDGE_PATH.equals(uri.getPath())
        ) {
            encodedParameters = uri.getRawFragment();
        } else {
            throw new IllegalArgumentException("Это не ссылка подключения Jarwis");
        }

        Map<String, String> parameters = decodeParameters(encodedParameters);
        String normalizedUrl = ServerAddress.normalize(parameters.get("url"));
        String code = parameters.getOrDefault("code", "").trim();
        if (!code.matches("\\d{8}")) {
            throw new IllegalArgumentException("В ссылке нет действующего восьмизначного кода");
        }
        return new PairingLink(normalizedUrl, code);
    }

    private static Map<String, String> decodeParameters(String encoded) {
        Map<String, String> result = new HashMap<>();
        if (encoded == null || encoded.isEmpty()) {
            return result;
        }
        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            result.putIfAbsent(key, value);
        }
        return result;
    }

    public String serverUrl() {
        return serverUrl;
    }

    public String pairingCode() {
        return pairingCode;
    }
}
