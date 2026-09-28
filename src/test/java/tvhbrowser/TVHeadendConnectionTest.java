package tvhbrowser;

import static org.junit.Assert.*;
import static tvhbrowser.TestSupport.browserWithSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class TVHeadendConnectionTest {

    private HttpServer server;
    private TVHeadendConnection connection;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();

    private void reply(HttpExchange ex, int status, String body) throws IOException {
        requests.add(ex.getRequestURI().toString());
        authHeaders.add(ex.getRequestHeaders().getFirst("Authorization"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Before
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/serverinfo", ex -> reply(ex, 200, "{\"version\":\"4.3-test\"}"));
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
        server.createContext("/api/dvr/entry/create_by_event", ex -> reply(ex, 200, "{}"));
        server.createContext("/api/dvr/entry/cancel", ex -> reply(ex, 200, "{}"));
        server.createContext("/api/idnode/save", ex -> reply(ex, 200, "{}"));
        server.start();

        connection = new TVHeadendConnection(browserWithSettings("127.0.0.1", server.getAddress().getPort()));
    }

    @After
    public void tearDown() {
        server.stop(0);
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
    public void requestsCarryBasicAuthHeader() {
        connection.testConnection();
        String expected = "Basic " + Base64.getEncoder().encodeToString("user:pw".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, authHeaders.get(0));
    }

    @Test
    public void testConnectionFailsOnHttpError() {
        server.removeContext("/api/serverinfo");
        server.createContext("/api/serverinfo", ex -> reply(ex, 401, "denied"));
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
    public void createTimerUsesEventId() {
        connection.createTimerWithEvent(123);
        assertTrue(requests.get(0).startsWith("/api/dvr/entry/create_by_event?event_id=123"));
    }

    @Test
    public void deleteTimerCallsCancelWithUuid() {
        connection.deleteTimer("abc");
        assertEquals("/api/dvr/entry/cancel?uuid=abc", requests.get(0));
    }

    @Test
    public void enableAndDisableSaveEnabledFlag() {
        connection.enableTimer("abc");
        connection.disableTimer("abc");
        assertTrue(requests.get(0).contains("%22enabled%22:true"));
        assertTrue(requests.get(0).contains("%22uuid%22:%22abc%22"));
        assertTrue(requests.get(1).contains("%22enabled%22:false"));
    }
}
