package com.vexsento.jarwis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class ServerAddress {
    public static final int DEFAULT_PORT = 8876;

    private ServerAddress() {
    }

    public static String normalize(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Укажи локальный адрес компьютера");
        }
        if (!value.contains("://")) {
            value = "http://" + value;
        }

        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Адрес компьютера записан неверно");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Разрешены только HTTP и HTTPS адреса");
        }
        String host = uri.getHost();
        if (host == null || !isPrivateIpv4(host)) {
            throw new IllegalArgumentException("Нужен локальный IPv4 адрес ПК, например 192.168.1.20");
        }
        int port = uri.getPort() < 0 ? DEFAULT_PORT : uri.getPort();
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Порт должен быть от 1 до 65535");
        }
        return scheme + "://" + host + ":" + port + "/";
    }

    public static boolean isPrivateIpv4(String host) {
        if (host == null || !host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
            return false;
        }
        String[] parts = host.split("\\.");
        int[] values = new int[4];
        for (int index = 0; index < parts.length; index++) {
            try {
                values[index] = Integer.parseInt(parts[index]);
            } catch (NumberFormatException error) {
                return false;
            }
            if (values[index] < 0 || values[index] > 255) {
                return false;
            }
        }
        return values[0] == 10
                || (values[0] == 172 && values[1] >= 16 && values[1] <= 31)
                || (values[0] == 192 && values[1] == 168)
                || values[0] == 127;
    }

    public static String origin(String normalizedUrl) {
        try {
            URI uri = new URI(normalizedUrl);
            return uri.getScheme().toLowerCase(Locale.ROOT)
                    + "://"
                    + uri.getHost().toLowerCase(Locale.ROOT)
                    + ":"
                    + uri.getPort();
        } catch (URISyntaxException error) {
            return "";
        }
    }
}

