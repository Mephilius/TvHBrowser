package tvhbrowser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class TVHeadendConnection {
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int EPG_READ_TIMEOUT_MS = 180_000;

    private final TvHBrowser tvhBrowser;
    private volatile boolean connected;
    private volatile boolean failureReported;

    public TVHeadendConnection(TvHBrowser tvhBrowser) {
        this.tvhBrowser = tvhBrowser;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean testConnection() {
        String urlString = baseUrl() + "/api/serverinfo";
        warnIfCredentialsAreNotEncrypted();

        try {
            String response = send("GET", urlString, null, READ_TIMEOUT_MS);
            JSONObject info = new JSONObject(response);
            String version = info.optString("sw_version", info.optString("version", "unknown"));
            tvhBrowser.writeLog("Connected to TVHeadend version " + version);
            connected = true;
            failureReported = false;
            return true;
        } catch (IOException | JSONException e) {
            tvhBrowser.writeLog("Connection test failed: " + urlString + ": " + e);
            showError(urlString + ": " + e);
        }

        connected = false;
        return false;
    }

    public List<TVHeadendChannel> getChannelList() {
        return fetchList("/api/channel/list", new TypeToken<List<TVHeadendChannel>>() {
        }.getType(), READ_TIMEOUT_MS);
    }

    public List<Timer> getUpcomingTimerList() {
        return fetchList("/api/dvr/entry/grid_upcoming?limit=2000", timerListType(), READ_TIMEOUT_MS);
    }

    public List<Timer> getAllTimerList() {
        return fetchList("/api/dvr/entry/grid", timerListType(), READ_TIMEOUT_MS);
    }

    public List<Timer> getFinishedTimerList() {
        return fetchList("/api/dvr/entry/grid_finished?limit=2000", timerListType(), READ_TIMEOUT_MS);
    }

    public List<Timer> getFailedTimerList() {
        return fetchList("/api/dvr/entry/grid_failed?limit=2000", timerListType(), READ_TIMEOUT_MS);
    }

    public List<EpgProgram> getEpgList() {
        return fetchList("/api/epg/events/grid?limit=50000", new TypeToken<List<EpgProgram>>() {
        }.getType(), EPG_READ_TIMEOUT_MS);
    }

    private static Type timerListType() {
        return new TypeToken<List<Timer>>() {
        }.getType();
    }

    /**
     * @return the id of the new timer or null if it could not be created
     */
    public String createTimerWithEvent(Integer eventId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("event_id", String.valueOf(eventId));
        // Tvheadend rejects the request without this parameter, an empty value selects the default configuration
        params.put("config_uuid", tvhBrowser.getSetting("dvrConfig", "").trim());

        String response = post("/api/dvr/entry/create_by_event", params);
        if (response == null) {
            return null;
        }
        try {
            JSONArray uuids = new JSONObject(response).optJSONArray("uuid");
            return uuids != null && uuids.length() > 0 ? uuids.getString(0) : null;
        } catch (JSONException e) {
            return null;
        }
    }

    public void deleteTimer(String timerId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("uuid", timerId);
        post("/api/dvr/entry/cancel", params);
    }

    public void disableTimer(String timerId) {
        setTimerEnabled(timerId, false);
    }

    public void enableTimer(String timerId) {
        setTimerEnabled(timerId, true);
    }

    private void setTimerEnabled(String timerId, boolean enabled) {
        JSONObject node = new JSONObject();
        node.put("enabled", enabled);
        node.put("uuid", timerId);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("node", node.toString());
        post("/api/idnode/save", params);
    }

    private <T> List<T> fetchList(String path, Type type, int readTimeoutMs) {
        String urlString = baseUrl() + path;
        try {
            String response = send("GET", urlString, null, readTimeoutMs);
            List<T> result = response.isEmpty() ? null
                    : new Gson().fromJson(new JSONObject(response).getJSONArray("entries").toString(), type);
            noteSuccess();
            return result == null ? new java.util.ArrayList<>() : result;
        } catch (IOException | JSONException | JsonParseException e) {
            noteFailure(urlString + ": " + e);
            return null;
        }
    }

    private String post(String path, Map<String, String> params) {
        String urlString = baseUrl() + path;
        try {
            String response = send("POST", urlString, encodeForm(params), READ_TIMEOUT_MS);
            tvhBrowser.writeLog("HTTP POST " + path + " " + params.keySet() + " returned " + response);
            return response;
        } catch (IOException e) {
            tvhBrowser.writeLog("HTTP POST " + urlString + " failed: " + e);
            showError(urlString + ": " + e);
            return null;
        }
    }

    private String send(String method, String urlString, String body, int readTimeoutMs) throws IOException {
        URL url;
        try {
            url = java.net.URI.create(urlString).toURL();
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL: " + urlString, e);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(tvhBrowser.getSetting("timeout", 500));
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("Authorization", authHeader());

            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error " + responseCode);
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                return buffer.toString(StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String encodeForm(Map<String, String> params) {
        StringBuilder form = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (form.length() > 0) {
                form.append('&');
            }
            form.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return form.toString();
    }

    private String baseUrl() {
        String protocol = tvhBrowser.getSetting("https", false) ? "https://" : "http://";
        return protocol + tvhBrowser.getSetting("fqdn", "tvheadend.local") + ":" + tvhBrowser.getSetting("port", 9981);
    }

    private String authHeader() {
        String credentials = tvhBrowser.getSetting("username", "") + ":" + tvhBrowser.getSetting("password", "");
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void warnIfCredentialsAreNotEncrypted() {
        if (!tvhBrowser.getSetting("https", false) && !tvhBrowser.getSetting("password", "").isEmpty()) {
            tvhBrowser.writeLog("WARNING: credentials are sent unencrypted, enable https in the settings");
        }
    }

    private void noteSuccess() {
        connected = true;
        if (failureReported) {
            failureReported = false;
            tvhBrowser.writeLog("Connection to Tvheadend restored");
        }
    }

    private void noteFailure(String message) {
        connected = false;
        tvhBrowser.writeLog("Request failed: " + message);
        if (!failureReported) {
            failureReported = true;
            showError(message);
        }
    }

    private void showError(String errorCode) {
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(null, "Error: " + errorCode, "Error", JOptionPane.ERROR_MESSAGE));
    }
}
