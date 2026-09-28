package tvhbrowser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.google.gson.Gson;

import devplugin.Plugin;
import devplugin.PluginManager;

public class ChannelManagerTest {

    private TvHBrowser browser;
    private TVHeadendConnection connection;
    private ChannelManager manager;
    private MockedStatic<Plugin> pluginStatic;
    private devplugin.Channel ard;
    private devplugin.Channel zdf;

    private static TVHeadendChannel tvhChannel(String key, String val) {
        return new Gson().fromJson("{\"key\":\"" + key + "\",\"val\":\"" + val + "\"}", TVHeadendChannel.class);
    }

    @Before
    public void setUp() {
        browser = mock(TvHBrowser.class);
        connection = mock(TVHeadendConnection.class);
        manager = new ChannelManager(browser, connection);

        ard = mock(devplugin.Channel.class);
        when(ard.getUniqueId()).thenReturn("tvb-ard");
        when(ard.getName()).thenReturn("ARD");
        zdf = mock(devplugin.Channel.class);
        when(zdf.getUniqueId()).thenReturn("tvb-zdf");
        when(zdf.getName()).thenReturn("ZDF");

        PluginManager pm = mock(PluginManager.class);
        when(pm.getSubscribedChannels()).thenReturn(new devplugin.Channel[] { ard, zdf });
        pluginStatic = mockStatic(Plugin.class);
        pluginStatic.when(Plugin::getPluginManager).thenReturn(pm);

        when(connection.getChannelList()).thenReturn(List.of(tvhChannel("k-ard", "Das Erste"),
                tvhChannel("k-zdf", "ZDF HD"), tvhChannel("k-3sat", "3sat")));
    }

    @After
    public void tearDown() {
        pluginStatic.close();
    }

    @Test
    public void importLoadsBothChannelLists() {
        manager.importChannelLists();
        assertEquals(2, manager.getTVBChannelList().size());
        assertEquals("Das Erste", manager.getTVHChannelbyKey("k-ard").getVal());
        assertSame(ard, manager.getTVBChannelbyID("tvb-ard"));
    }

    @Test
    public void mapChannelsCreatesBidirectionalMapping() {
        manager.importChannelLists();
        manager.mapChannels("tvb-ard", "k-ard");
        assertSame(ard, manager.getTVBChannelbyTVHChannelKey("k-ard"));
        assertEquals("k-ard", manager.getMappedTvhChannel("tvb-ard").getKey());
        assertSame(ard, manager.getMappedTvbChannel("k-ard"));
    }

    @Test
    public void unmappedLookupsReturnNull() {
        manager.importChannelLists();
        assertNull(manager.getTVBChannelbyTVHChannelKey("k-ard"));
        assertNull(manager.getMappedTvhChannel("tvb-ard"));
        assertNull(manager.getMappedTvbChannel("k-ard"));
    }

    @Test
    public void unmappedTvhChannelsExcludeMappedOnes() {
        manager.importChannelLists();
        manager.mapChannels("tvb-ard", "k-ard");
        List<String> keys = manager.getUnmappedTVHChannels().stream().map(TVHeadendChannel::getKey).sorted()
                .collect(Collectors.toList());
        assertEquals(List.of("k-3sat", "k-zdf"), keys);
    }

    @Test
    public void unmappedChannelsLoadTvhChannelsLazily() {
        assertEquals(3, manager.getUnmappedTVHChannels().size());
        verify(connection).getChannelList();
    }

    @Test
    public void tvbChannelListLoadsLazily() {
        assertEquals(2, manager.getTVBChannelList().size());
    }

    @Test
    public void mapByNameResolvesBothSides() {
        manager.importChannelLists();
        manager.mapChannelsByName("ZDF", "ZDF HD");
        assertSame(zdf, manager.getTVBChannelbyTVHChannelKey("k-zdf"));
        verify(browser, never()).showError(anyString());
    }

    @Test
    public void mapByNameReportsUnknownChannels() {
        manager.importChannelLists();
        manager.mapChannelsByName("Nope", "Nope");
        verify(browser, times(2)).showError(anyString());
    }

    @Test
    public void unmapByNameRemovesMapping() {
        manager.importChannelLists();
        manager.mapChannelsByName("ARD", "Das Erste");
        manager.unmapChannelsByName("ARD", "Das Erste");
        assertNull(manager.getTVBChannelbyTVHChannelKey("k-ard"));
        assertNull(manager.getMappedTvhChannel("tvb-ard"));
    }

    @Test
    public void unmapByNameReportsUnknownChannels() {
        manager.importChannelLists();
        manager.unmapChannelsByName("Nope", "Nope");
        verify(browser, times(2)).showError(anyString());
    }

    @Test
    public void writeAndReadDataRoundTrip() throws Exception {
        manager.importChannelLists();
        manager.mapChannels("tvb-ard", "k-ard");
        manager.mapChannels("tvb-zdf", "k-zdf");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            manager.writeData(out);
        }

        ChannelManager fresh = new ChannelManager(browser, connection);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            fresh.readData(in);
        }
        assertSame(ard, fresh.getTVBChannelbyTVHChannelKey("k-ard"));
        assertSame(zdf, fresh.getTVBChannelbyTVHChannelKey("k-zdf"));
    }
}
