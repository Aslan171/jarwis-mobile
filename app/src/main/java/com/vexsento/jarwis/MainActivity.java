package com.vexsento.jarwis;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity {
    private static final String PREFS_NAME = "jarwis_mobile";
    private static final String PREF_SERVER_URL = "server_url";
    private static final int FILE_CHOOSER_REQUEST = 7001;
    private static final int MICROPHONE_REQUEST = 7002;

    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private ExecutorService scanExecutor;
    private SharedPreferences preferences;
    private View setupPanel;
    private View browserPanel;
    private WebView webView;
    private EditText addressInput;
    private TextView connectionStatus;
    private ProgressBar progressBar;
    private Button qrScanButton;
    private Button scanButton;
    private String currentBaseUrl = "";
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setupPanel = findViewById(R.id.setup_panel);
        browserPanel = findViewById(R.id.browser_panel);
        webView = findViewById(R.id.web_view);
        addressInput = findViewById(R.id.server_address);
        connectionStatus = findViewById(R.id.connection_status);
        progressBar = findViewById(R.id.connection_progress);
        qrScanButton = findViewById(R.id.qr_scan_button);
        scanButton = findViewById(R.id.scan_button);
        Button connectButton = findViewById(R.id.connect_button);
        ImageButton settingsButton = findViewById(R.id.server_settings_button);

        configureWebView();
        qrScanButton.setOnClickListener(view -> startQrScanner());
        connectButton.setOnClickListener(view -> connectTo(addressInput.getText().toString(), false));
        scanButton.setOnClickListener(view -> startNetworkScan());
        settingsButton.setOnClickListener(view -> showSetup("Можно выбрать другой компьютер Jarwis."));

        PairingLink pairingLink = pairingLinkFromIntent(getIntent());
        String savedUrl = preferences.getString(PREF_SERVER_URL, "");
        if (pairingLink != null) {
            pairAndConnect(pairingLink);
        } else if (!savedUrl.isEmpty()) {
            addressInput.setText(savedUrl);
            connectTo(savedUrl, true);
        } else {
            showSetup("Нажми «Сканировать QR» и наведи камеру на код, открытый на компьютере.");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        PairingLink pairingLink = pairingLinkFromIntent(intent);
        if (pairingLink != null) {
            pairAndConnect(pairingLink);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDatabaseEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(settings.getUserAgentString() + " JarwisAndroid/1.1");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.setBackgroundColor(Color.BLACK);
        webView.setWebViewClient(new JarwisWebViewClient());
        webView.setWebChromeClient(new JarwisChromeClient());
        webView.setDownloadListener(new ExternalDownloadListener());
    }

    private void connectTo(String rawAddress, boolean quietFailure) {
        final String normalized;
        try {
            normalized = ServerAddress.normalize(rawAddress);
        } catch (IllegalArgumentException error) {
            showSetup(error.getMessage());
            return;
        }

        setBusy(true, "Проверяю " + normalized.replace("http://", "") + "…");
        String sessionCookie = CookieManager.getInstance().getCookie(normalized);
        ioExecutor.execute(() -> {
            boolean available = probeJarwis(normalized);
            boolean authorized = !quietFailure || probeAuthorizedJarwis(normalized, sessionCookie);
            runOnUiThread(() -> {
                if (available && authorized) {
                    openJarwis(normalized);
                } else if (available) {
                    showSetup("Компьютер найден, но вход ещё не подтверждён. Нажми «Сканировать QR».");
                } else {
                    String message = quietFailure
                            ? "Сохранённый компьютер сейчас недоступен. Ищу другой Jarwis в сети…"
                            : "Jarwis не отвечает. Запусти start_jarwis_mobile.cmd на ПК и проверь одну Wi-Fi сеть.";
                    showSetup(message);
                    if (quietFailure) {
                        startNetworkScan();
                    }
                }
            });
        });
    }

    private void pairAndConnect(PairingLink link) {
        cancelScan();
        addressInput.setText(link.serverUrl());
        setupPanel.setVisibility(View.VISIBLE);
        browserPanel.setVisibility(View.GONE);
        setBusy(true, "Подключаю телефон к Jarwis…");
        ioExecutor.execute(() -> {
            PairingResult result = requestPairing(link);
            runOnUiThread(() -> {
                if (!result.connected) {
                    showSetup(result.errorMessage);
                    return;
                }
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setCookie(link.serverUrl(), result.cookie, accepted -> {
                    if (!Boolean.TRUE.equals(accepted)) {
                        showSetup("Android не сохранил защищённую сессию. Повтори сканирование QR-кода.");
                        return;
                    }
                    cookieManager.flush();
                    openJarwis(link.serverUrl());
                });
            });
        });
    }

    private void startQrScanner() {
        cancelScan();
        setBusy(true, "Открываю камеру…");
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue == null || rawValue.isBlank()) {
                        showSetup("QR-код пустой. Отсканируй код, который Jarwis показывает на компьютере.");
                        return;
                    }
                    try {
                        pairAndConnect(PairingLink.parse(rawValue));
                    } catch (IllegalArgumentException error) {
                        showSetup("Это не QR-код подключения Jarwis. Открой мобильный режим на компьютере и попробуй снова.");
                    }
                })
                .addOnCanceledListener(() -> showSetup("Сканирование отменено."))
                .addOnFailureListener(error -> showSetup(
                        "Не удалось открыть сканер. Проверь Google Play Services или используй обычную камеру телефона."
                ));
    }

    private PairingResult requestPairing(PairingLink link) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(
                    link.serverUrl() + "api/mobile/pair"
            ).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(3500);
            connection.setReadTimeout(4500);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] body = ("{\"code\":\"" + link.pairingCode() + "\"}").getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_FORBIDDEN) {
                return PairingResult.error("QR-код неверный или истёк. Перезапусти мобильный режим на ПК и отсканируй новый QR.");
            }
            if (status != HttpURLConnection.HTTP_OK) {
                return PairingResult.error("ПК отклонил подключение. Перезапусти start_jarwis_mobile.cmd и попробуй снова.");
            }
            String cookie = connection.getHeaderField("Set-Cookie");
            if (cookie == null || !cookie.startsWith("jarwis_mobile_session=")) {
                return PairingResult.error("ПК не выдал защищённую сессию. Перезапусти мобильный режим Jarwis.");
            }
            return PairingResult.connected(cookie);
        } catch (Exception error) {
            return PairingResult.error("Телефон не видит ПК. Проверь, что оба устройства подключены к одной Wi-Fi сети.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void startNetworkScan() {
        cancelScan();
        String prefix = localIpv4Prefix();
        if (prefix.isEmpty()) {
            showSetup("Не удалось определить локальную IPv4 сеть. Введи адрес ПК вручную.");
            return;
        }

        setBusy(true, "Ищу Jarwis в сети " + prefix + "0/24…");
        scanButton.setEnabled(false);
        scanExecutor = Executors.newFixedThreadPool(32);
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger remaining = new AtomicInteger(254);

        for (int suffix = 1; suffix <= 254; suffix++) {
            final String candidate = "http://" + prefix + suffix + ":" + ServerAddress.DEFAULT_PORT + "/";
            scanExecutor.submit(() -> {
                try {
                    if (!found.get() && probeJarwis(candidate) && found.compareAndSet(false, true)) {
                        runOnUiThread(() -> {
                            addressInput.setText(candidate);
                            showSetup("Компьютер найден. Для защищённого входа нажми «Сканировать QR».");
                            cancelScan();
                        });
                    }
                } finally {
                    if (remaining.decrementAndGet() == 0 && !found.get()) {
                        runOnUiThread(() -> {
                            cancelScan();
                            showSetup(
                                    "Jarwis не найден. Запусти мобильный режим на ПК и введи показанный IPv4 адрес вручную."
                            );
                        });
                    }
                }
            });
        }
    }

    private boolean probeJarwis(String baseUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(baseUrl + "manifest.webmanifest").toURL().openConnection();
            connection.setConnectTimeout(550);
            connection.setReadTimeout(750);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/manifest+json");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null && body.length() < 8192) {
                    body.append(line);
                }
                return body.toString().contains("Jarwis 1.0");
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean probeAuthorizedJarwis(String baseUrl, String sessionCookie) {
        if (sessionCookie == null || !sessionCookie.contains("jarwis_mobile_session=")) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(baseUrl + "api/bootstrap").toURL().openConnection();
            connection.setConnectTimeout(900);
            connection.setReadTimeout(1200);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cookie", sessionCookie);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String localIpv4Prefix() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = manager.getActiveNetwork();
        LinkProperties properties = activeNetwork == null ? null : manager.getLinkProperties(activeNetwork);
        if (properties == null) {
            return "";
        }
        for (LinkAddress linkAddress : properties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            String host = address.getHostAddress();
            if (address instanceof Inet4Address && ServerAddress.isPrivateIpv4(host) && !host.startsWith("127.")) {
                int split = host.lastIndexOf('.');
                return split > 0 ? host.substring(0, split + 1) : "";
            }
        }
        return "";
    }

    private void openJarwis(String normalizedUrl) {
        cancelScan();
        currentBaseUrl = normalizedUrl;
        preferences.edit().putString(PREF_SERVER_URL, normalizedUrl).apply();
        addressInput.setText(normalizedUrl);
        setupPanel.setVisibility(View.GONE);
        browserPanel.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        webView.loadUrl(normalizedUrl);
    }

    private void showSetup(String message) {
        setBusy(false, message);
        browserPanel.setVisibility(View.GONE);
        setupPanel.setVisibility(View.VISIBLE);
        qrScanButton.setEnabled(true);
        scanButton.setEnabled(true);
    }

    private void setBusy(boolean busy, String message) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        connectionStatus.setText(message);
        if (qrScanButton != null) {
            qrScanButton.setEnabled(!busy);
        }
    }

    private void cancelScan() {
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
            scanExecutor = null;
        }
        if (scanButton != null) {
            scanButton.setEnabled(true);
        }
    }

    private PairingLink pairingLinkFromIntent(Intent intent) {
        if (intent == null || intent.getDataString() == null) {
            return null;
        }
        try {
            return PairingLink.parse(intent.getDataString());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static final class PairingResult {
        private final boolean connected;
        private final String cookie;
        private final String errorMessage;

        private PairingResult(boolean connected, String cookie, String errorMessage) {
            this.connected = connected;
            this.cookie = cookie;
            this.errorMessage = errorMessage;
        }

        private static PairingResult connected(String cookie) {
            return new PairingResult(true, cookie, "");
        }

        private static PairingResult error(String message) {
            return new PairingResult(false, "", message);
        }
    }

    private boolean isCurrentOrigin(String value) {
        try {
            String normalized = ServerAddress.normalize(value);
            return ServerAddress.origin(normalized).equals(ServerAddress.origin(currentBaseUrl));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "На телефоне нет приложения для этой ссылки", Toast.LENGTH_SHORT).show();
        }
    }

    private final class JarwisWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (isCurrentOrigin(url)) {
                return false;
            }
            openExternal(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                showSetup("Связь с Jarwis потеряна. Проверь ПК и повтори подключение.");
            }
        }
    }

    private final class JarwisChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
        ) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(null);
            }
            fileCallback = callback;
            Intent chooser = params.createIntent();
            chooser.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                startActivityForResult(Intent.createChooser(chooser, "Выбрать файл"), FILE_CHOOSER_REQUEST);
                return true;
            } catch (ActivityNotFoundException error) {
                fileCallback = null;
                return false;
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (!ServerAddress.origin(request.getOrigin().toString()).equals(ServerAddress.origin(currentBaseUrl))) {
                request.deny();
                return;
            }
            for (String resource : request.getResources()) {
                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                    } else {
                        pendingPermissionRequest = request;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_REQUEST);
                    }
                    return;
                }
            }
            request.deny();
        }
    }

    private final class ExternalDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(
                String url,
                String userAgent,
                String contentDisposition,
                String mimeType,
                long contentLength
        ) {
            openExternal(url);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MICROPHONE_REQUEST && pendingPermissionRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            } else {
                pendingPermissionRequest.deny();
            }
            pendingPermissionRequest = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (browserPanel.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        cancelScan();
        ioExecutor.shutdownNow();
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        webView.destroy();
        super.onDestroy();
    }
}
