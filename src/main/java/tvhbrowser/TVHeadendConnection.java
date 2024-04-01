package tvhbrowser;

import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

public class TVHeadendConnection {
    private final TvHBrowser tvhBrowser;
    private boolean connected;

    public TVHeadendConnection(TvHBrowser tvhBrowser) {
        this.tvhBrowser = tvhBrowser;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean testConnection() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/serverinfo";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);

            String authString = username + ":" + password;
            String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthString);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                String version = jsonObject.getString("version");
                tvhBrowser.writeLog("Connected to TVHeadend version " + version);
                connected = true;
                return true;
            } else {
                showError("Calling " + urlString + " - HTTP error " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        }

        connected = false;
        return false;
    }

    private JSONArray makeHttpRequest(String urlString, String username, String password, int timeout) {
        JSONArray entries = new JSONArray();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);

            String authString = username + ":" + password;
            String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthString);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (response.length() > 0) {
                    JSONObject jsonObject = new JSONObject(response.toString());
                    entries = jsonObject.getJSONArray("entries");
                }
            } else {
                showError("Calling " + urlString + " - HTTP error " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        }

        tvhBrowser.writeLog("HTTP request to " + urlString + " returned " + entries.length() + " entries");
        return entries;
    }

    public List<TVHeadendChannel> getChannelList() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/channel/list";

        JSONArray response = makeHttpRequest(urlString, username, password, timeout);

        Gson gson = new Gson();
        Type type = new TypeToken<List<TVHeadendChannel>>() {
        }.getType();
        List<TVHeadendChannel> Channels = gson.fromJson(response.toString(), type);

        return Channels;
    }

    public List<Timer> getUpcomingTimerList() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/grid_upcoming?limit=2000";

        JSONArray response = makeHttpRequest(urlString, username, password, timeout);

        Gson gson = new Gson();
        Type type = new TypeToken<List<Timer>>() {
        }.getType();
        List<Timer> timers = gson.fromJson(response.toString(), type);

        return timers;
    }

    public List<Timer> getAllTimerList() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/grid";

        JSONArray response = makeHttpRequest(urlString, username, password, timeout);

        Gson gson = new Gson();
        Type type = new TypeToken<List<Timer>>() {
        }.getType();
        List<Timer> timers = gson.fromJson(response.toString(), type);

        return timers;
    }

    public List<Timer> getFinishedTimerList() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/grid_finished?limit=2000";

        JSONArray response = makeHttpRequest(urlString, username, password, timeout);

        Gson gson = new Gson();
        Type type = new TypeToken<List<Timer>>() {
        }.getType();
        List<Timer> timers = gson.fromJson(response.toString(), type);

        return timers;
    }

    public List<Timer> getFailedTimerList() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/grid_failed?limit=2000";

        JSONArray response = makeHttpRequest(urlString, username, password, timeout);

        Gson gson = new Gson();
        Type type = new TypeToken<List<Timer>>() {
        }.getType();
        List<Timer> timers = gson.fromJson(response.toString(), type);

        return timers;
    }


    public void createTimerWithEvent(Integer eventId) {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/create_by_event?event_id=" + eventId + "&config_uuid=c8bcf2c90db93380f1861773d92b8e8f";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);

            String authString = username + ":" + password;
            String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthString);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                tvhBrowser.writeLog("HTTP request to " + urlString + " returned " + response.toString());
            } else {
                showError("Calling " + urlString + " - HTTP error " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        }
    }

    public void deleteTimer(String timerId) {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/cancel?uuid=" +  timerId;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);

            String authString = username + ":" + password;
            String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthString);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                tvhBrowser.writeLog("HTTP request to " + urlString + " returned " + response.toString());
            } else {
                showError("Calling " + urlString + " - HTTP error " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        }
    }

    public void disableTimer(String timerId) {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/dvr/entry/cancel?uuid=" + timerId;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);

            String authString = username + ":" + password;
            String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthString);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                tvhBrowser.writeLog("HTTP request to " + urlString + " returned " + response.toString());
            } else {
                showError("Calling " + urlString + " - HTTP error " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError(urlString + ": " + e.toString());
        }
    }

    public List<EpgProgram> getEpgList() {
        String fqdn = tvhBrowser.getSetting("fqdn", "tvheadend.local");
        int port = tvhBrowser.getSetting("port", 9981);
        int timeout = tvhBrowser.getSetting("timeout", 500);
        String username = tvhBrowser.getSetting("username", "");
        String password = tvhBrowser.getSetting("password", "");
        boolean useHttps = tvhBrowser.getSetting("https", false);

        String protocol = useHttps ? "https://" : "http://";
        String urlString = protocol + fqdn + ":" + port + "/api/epg/events/grid?limit=50000";

        JSONArray response = makeHttpRequest(urlString, username, password, timeout);

        Gson gson = new Gson();
        Type type = new TypeToken<List<EpgProgram>>() {
        }.getType();
        List<EpgProgram> programs = gson.fromJson(response.toString(), type);

        return programs;
    }

    private void showError(String errorCode) {
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(null, "Error: " + errorCode, "Error", JOptionPane.ERROR_MESSAGE));
    }
}
