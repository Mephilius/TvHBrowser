package tvhbrowser;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static tvhbrowser.TestSupport.browserWithSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class TVHeadendConnectionTest {

    private HttpServer server;
    private TvHBrowser browser;
    private TVHeadendConnection connection;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();
    private final List<String> methods = new CopyOnWriteArrayList<>();
    private final List<String> bodies = new CopyOnWriteArrayList<>();

    private void reply(HttpExchange ex, int status, String body) throws IOException {
        requests.add(ex.getRequestURI().toString());
        authHeaders.add(ex.getRequestHeaders().getFirst("Authorization"));
        methods.add(ex.getRequestMethod());
        bodies.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void respond(String path, int status, String body) {
        server.removeContext(path);
        server.createContext(path, ex -> reply(ex, status, body));
    }

    @Before
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/serverinfo", ex -> reply(ex, 200,
                "{\"sw_version\":\"4.3-test\",\"api_version\":20,\"name\":\"Tvheadend\"}"));
        server.createContext("/api/channel/list", ex -> reply(ex, 200,
                "{\"entries\":[{\"key\":\"k1\",\"val\":\"ARD\"},{\"key\":\"k2\",\"val\":\"ZDF\"}]}"));
        server.createContext("/api/dvr/entry/grid_upcoming", ex -> reply(ex, 200,
                "{\"entries\":[{\"uuid\":\"u1\",\"enabled\":true,\"start\":1000,\"title\":{\"ger\":\"Tatort\"}}]}"));
        server.createContext("/api/dvr/entry/grid_finished", ex -> reply(ex, 200,
                "{\"entries\":[{\"uuid\":\"u2\"},{\"uuid\":\"u3\"}]}"));
        server.createContext("/api/dvr/entry/grid_failed", ex -> reply(ex, 200, "{\"entries\":[{\"uuid\":\"u4\"}]}"));
        server.createContext("/api/dvr/entry/grid", ex -> reply(ex, 200, "{\"entries\":[{\"uuid\":\"u5\"}]}"));
        server.createContext("/api/epg/events/grid", ex -> reply(ex, 200,
                "{\"entries\":[{\"eventId\":9,\"channelUuid\":\"k1\",\"start\":1000,\"title\":\"Tatort\"}]}"));
        server.createContext("/api/dvr/entry/create_by_event",
                ex -> reply(ex, 200, "{\"uuid\":[\"new-uuid\"]}"));
        server.createContext("/api/dvr/entry/cancel", ex -> reply(ex, 200, "{}"));
        server.createContext("/api/idnode/save", ex -> reply(ex, 200, "{}"));
        server.start();

        browser = browserWithSettings("127.0.0.1", server.getAddress().getPort());
        connection = new TVHeadendConnection(browser);
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    private static String nodeOf(String body) {
        return URLDecoder.decode(body.substring("node=".length()), StandardCharsets.UTF_8);
    }

    @Test
    public void notConnectedInitially() {
        assertFalse(connection.isConnected());
    }

    @Test
    public void testConnectionSucceedsAndSetsConnected() {
        assertTrue(connection.testConnection());
        assertTrue(connection.isConnected());
    }

    @Test
    public void testConnectionAcceptsOlderServerInfoWithVersionField() {
        respond("/api/serverinfo", 200, "{\"version\":\"4.0\"}");
        assertTrue(connection.testConnection());
    }

    @Test
    public void testConnectionAcceptsServerInfoWithoutAnyVersion() {
        respond("/api/serverinfo", 200, "{\"name\":\"Tvheadend\"}");
        assertTrue(connection.testConnection());
    }

    @Test
    public void testConnectionFailsOnInvalidJson() {
        respond("/api/serverinfo", 200, "<html>login</html>");
        assertFalse(connection.testConnection());
    }

    @Test
    public void requestsCarryBasicAuthHeader() {
        connection.testConnection();
        String expected = "Basic " + Base64.getEncoder().encodeToString("user:pw".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, authHeaders.get(0));
    }

    @Test
    public void testConnectionFailsOnHttpError() {
        respond("/api/serverinfo", 401, "denied");
        assertFalse(connection.testConnection());
        assertFalse(connection.isConnected());
    }

    @Test
    public void channelListIsParsed() {
        List<TVHeadendChannel> channels = connection.getChannelList();
        assertEquals(2, channels.size());
        assertEquals("k1", channels.get(0).getKey());
        assertEquals("ZDF", channels.get(1).getVal());
    }

    @Test
    public void upcomingTimersAreParsedWithLimit() {
        List<Timer> timers = connection.getUpcomingTimerList();
        assertEquals(1, timers.size());
        assertEquals("u1", timers.get(0).getUuid());
        assertTrue(timers.get(0).isEnabled());
        assertEquals("Tatort", timers.get(0).getTitle("ger"));
        assertTrue(requests.get(0).contains("limit=2000"));
        assertEquals("GET", methods.get(0));
    }

    @Test
    public void finishedFailedAndAllTimersUseTheirEndpoints() {
        assertEquals(2, connection.getFinishedTimerList().size());
        assertEquals(1, connection.getFailedTimerList().size());
        assertEquals(1, connection.getAllTimerList().size());
        assertTrue(requests.get(0).startsWith("/api/dvr/entry/grid_finished"));
        assertTrue(requests.get(1).startsWith("/api/dvr/entry/grid_failed"));
        assertTrue(requests.get(2).startsWith("/api/dvr/entry/grid"));
    }

    @Test
    public void epgListIsParsed() {
        List<EpgProgram> epg = connection.getEpgList();
        assertEquals(1, epg.size());
        assertEquals(9, epg.get(0).getEventId());
        assertTrue(requests.get(0).contains("limit=50000"));
    }

    @Test
    public void emptyEntriesGiveEmptyListNotNull() {
        respond("/api/channel/list", 200, "{\"entries\":[]}");
        assertTrue(connection.getChannelList().isEmpty());
    }

    @Test
    public void createTimerPostsEventIdWithEmptyConfigForTheDefault() {
        assertEquals("new-uuid", connection.createTimerWithEvent(123));
        assertEquals("POST", methods.get(0));
        assertEquals("/api/dvr/entry/create_by_event", requests.get(0));
        assertEquals("event_id=123&config_uuid=", bodies.get(0));
    }

    @Test
    public void createTimerPassesConfiguredDvrConfig() {
        when(browser.getSetting(eq("dvrConfig"), anyString())).thenReturn(" abc def ");
        connection.createTimerWithEvent(5);
        assertEquals("event_id=5&config_uuid=abc+def", bodies.get(0));
    }

    @Test
    public void createTimerReturnsNullWhenServerRejects() {
        respond("/api/dvr/entry/create_by_event", 500, "boom");
        assertNull(connection.createTimerWithEvent(1));
    }

    @Test
    public void deleteTimerPostsUuid() {
        connection.deleteTimer("abc");
        assertEquals("POST", methods.get(0));
        assertEquals("/api/dvr/entry/cancel", requests.get(0));
        assertEquals("uuid=abc", bodies.get(0));
    }

    @Test
    public void uuidIsUrlEncoded() {
        connection.deleteTimer("a&b=c d");
        assertEquals("uuid=a%26b%3Dc+d", bodies.get(0));
    }

    @Test
    public void enableAndDisableSaveEnabledFlagAsJson() {
        connection.enableTimer("abc");
        connection.disableTimer("abc");
        assertEquals("POST", methods.get(0));
        assertEquals("/api/idnode/save", requests.get(0));
        JSONObject enabled = new JSONObject(nodeOf(bodies.get(0)));
        assertTrue(enabled.getBoolean("enabled"));
        assertEquals("abc", enabled.getString("uuid"));
        assertFalse(new JSONObject(nodeOf(bodies.get(1))).getBoolean("enabled"));
    }

    @Test
    public void quotesInTimerIdCannotBreakTheJson() {
        connection.enableTimer("x\",\"enabled\":false,\"y\":\"");
        JSONObject node = new JSONObject(nodeOf(bodies.get(0)));
        assertTrue(node.getBoolean("enabled"));
        assertEquals(2, node.length());
    }

    @Test
    public void listRequestsReturnNullOnHttpError() {
        respond("/api/dvr/entry/grid_upcoming", 500, "boom");
        assertNull(connection.getUpcomingTimerList());
    }

    @Test
    public void listRequestsReturnNullOnInvalidJson() {
        respond("/api/channel/list", 200, "not json");
        assertNull(connection.getChannelList());
    }

    @Test
    public void unreachableServerGivesNullInsteadOfAnException() {
        server.stop(0);
        assertNull(connection.getUpcomingTimerList());
        assertNull(connection.getUpcomingTimerList());
        assertFalse(connection.isConnected());
    }

    @Test
    public void connectionIsRestoredAfterSuccessfulRequest() {
        respond("/api/dvr/entry/grid_upcoming", 500, "boom");
        assertNull(connection.getUpcomingTimerList());
        assertFalse(connection.isConnected());

        respond("/api/dvr/entry/grid_upcoming", 200, "{\"entries\":[]}");
        assertNotNull(connection.getUpcomingTimerList());
        assertTrue(connection.isConnected());
    }
}
