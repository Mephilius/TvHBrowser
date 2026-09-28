package tvhbrowser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static tvhbrowser.TestSupport.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import devplugin.Channel;
import devplugin.Date;
import devplugin.Program;

public class EpgManagerTest {

    private static final long START = epoch(2026, 9, 28, 20, 15);
    private static final int START_MIN = 20 * 60 + 15;

    private TvHBrowser browser;
    private TVHeadendConnection connection;
    private ChannelManager channels;
    private EpgManager manager;

    private static EpgProgram epg(int id, String channelUuid, long start, String title) {
        EpgProgram e = new EpgProgram();
        e.setEventId(id);
        e.setChannelUuid(channelUuid);
        e.setStart(start);
        e.setTitle(title);
        return e;
    }

    private Program programOnChannel(String title, int minute, long startDay) {
        Program p = program(title, minute);
        Channel c = mock(Channel.class);
        when(c.getUniqueId()).thenReturn("tvb-ard");
        when(p.getChannel()).thenReturn(c);
        when(p.getDate()).thenReturn(new Date(2026, 9, 28));
        return p;
    }

    @Before
    public void setUp() {
        browser = mock(TvHBrowser.class);
        connection = mock(TVHeadendConnection.class);
        channels = mock(ChannelManager.class);
        manager = new EpgManager(browser, connection, channels);

        TVHeadendChannel tvh = new Gson().fromJson("{\"key\":\"ch-uuid\",\"val\":\"ARD\"}", TVHeadendChannel.class);
        when(channels.getMappedTvhChannel("tvb-ard")).thenReturn(tvh);
    }

    @Test
    public void loadEpgListIndexesByEventId() {
        EpgProgram e = epg(1, "ch-uuid", START, "Tatort");
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(e)));
        manager.loadEpgList();
        assertSame(e, manager.getEpgProgram(1));
        assertNull(manager.getEpgProgram(2));
    }

    @Test
    public void loadEpgListReplacesOldContent() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(epg(1, "c", START, "A"))))
                .thenReturn(new ArrayList<>(List.of(epg(2, "c", START, "B"))));
        manager.loadEpgList();
        manager.updateEpg();
        assertNull(manager.getEpgProgram(1));
        assertNotNull(manager.getEpgProgram(2));
    }

    @Test
    public void findsEpgForProgramByChannelDateAndTitle() {
        EpgProgram match = epg(1, "ch-uuid", START, "Tatort");
        EpgProgram other = epg(2, "ch-uuid", START + 7200, "Anderes");
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(other, match)));
        manager.loadEpgList();

        assertSame(match, manager.getEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
    }

    @Test
    public void returnsNullWhenNoEntryMatches() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(epg(1, "ch-uuid", START, "Tatort"))));
        manager.loadEpgList();

        assertNull(manager.getEpgProgram(programOnChannel("Heute", START_MIN, 0)));
    }

    @Test
    public void returnsNullAndWarnsWhenNoEpgForDate() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>());
        manager.loadEpgList();

        assertNull(manager.getEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
        verify(browser).showWarning(anyString());
    }

    @Test
    public void returnsNullAndWarnsWhenNoEpgForChannel() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(epg(1, "other-channel", START, "Tatort"))));
        manager.loadEpgList();

        assertNull(manager.getEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
        verify(browser).showWarning(anyString());
    }

    @Test
    public void returnsNullWhenChannelNotMapped() {
        when(channels.getMappedTvhChannel("tvb-ard")).thenReturn(null);
        assertNull(manager.getEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
    }

    @Test
    public void nullProgramShowsError() {
        assertNull(manager.getEpgProgram((Program) null));
        verify(browser).showError(anyString());
    }
}
