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

    @Test
    public void strictLookupDoesNotUseTimeFallback() {
        when(connection.getEpgList())
                .thenReturn(new ArrayList<>(List.of(epg(1, "ch-uuid", START - 900, "Tatort"))));
        manager.loadEpgList();

        assertNull(manager.getEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
    }

    @Test
    public void similarReturnsNearestTextMatchWithinOneHour() {
        EpgProgram farther = epg(1, "ch-uuid", START - 3000, "Tatort");
        EpgProgram nearer = epg(2, "ch-uuid", START - 900, "Tatort");
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(farther, nearer)));
        manager.loadEpgList();

        assertSame(nearer, manager.getSimilarEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
    }

    @Test
    public void similarIsNullBeyondOneHourOrWithOtherTitle() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(
                epg(1, "ch-uuid", START - 3 * 3600, "Tatort"), epg(2, "ch-uuid", START, "Anderes"))));
        manager.loadEpgList();

        assertNull(manager.getSimilarEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
    }

    @Test
    public void surroundingReturnsRunningAndNextEvent() {
        EpgProgram earlier = epg(1, "ch-uuid", START - 7200, "Frueher");
        EpgProgram running = epg(2, "ch-uuid", START - 900, "Laeuft");
        EpgProgram next = epg(3, "ch-uuid", START + 1800, "Danach");
        EpgProgram later = epg(4, "ch-uuid", START + 7200, "Spaeter");
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(later, earlier, next, running)));
        manager.loadEpgList();

        assertEquals(List.of(running, next), manager.getSurroundingEpgPrograms(programOnChannel("X", START_MIN, 0)));
    }

    @Test
    public void surroundingHandlesMissingNeighbour() {
        EpgProgram only = epg(1, "ch-uuid", START + 600, "Danach");
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(only)));
        manager.loadEpgList();

        assertEquals(List.of(only), manager.getSurroundingEpgPrograms(programOnChannel("X", START_MIN, 0)));
    }

    @Test
    public void surroundingIsEmptyWithoutEpgData() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>());
        manager.loadEpgList();

        assertTrue(manager.getSurroundingEpgPrograms(programOnChannel("X", START_MIN, 0)).isEmpty());
        verify(browser, never()).showWarning(anyString());
    }

    @Test
    public void notReadyBeforeFirstLoadAndReadyAfterwards() {
        assertFalse(manager.isReady());
        when(connection.getEpgList()).thenReturn(new ArrayList<>());
        manager.loadEpgList();
        assertTrue(manager.isReady());
    }

    @Test
    public void readyEvenWhenLoadingFails() {
        when(connection.getEpgList()).thenThrow(new IllegalStateException("boom"));
        try {
            manager.loadEpgList();
            fail();
        } catch (IllegalStateException expected) {
        }
        assertTrue(manager.isReady());
    }

    @Test
    public void reloadKeepsOldDataUntilNewDataIsComplete() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(epg(1, "ch-uuid", START, "A"))))
                .thenThrow(new IllegalStateException("boom"));
        manager.loadEpgList();
        try {
            manager.updateEpg();
        } catch (IllegalStateException expected) {
        }
        assertNotNull(manager.getEpgProgram(1));
    }

    @Test
    public void nullEpgListIsTreatedAsEmpty() {
        when(connection.getEpgList()).thenReturn(null);
        manager.loadEpgList();
        assertTrue(manager.isReady());
        assertNull(manager.getEpgProgram(1));
    }

    @Test
    public void failedReloadKeepsPreviousData() {
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(epg(1, "ch-uuid", START, "A"))))
                .thenReturn(null);
        manager.loadEpgList();
        manager.updateEpg();
        assertNotNull(manager.getEpgProgram(1));
        assertTrue(manager.isReady());
    }

    @Test
    public void getEpgProgramPicksBestScoringCandidateNotFirstMatchingOne() {
        EpgProgram prefixMatch = epg(1, "ch-uuid", START, "Tatort: Der Fall");
        EpgProgram exactMatch = epg(2, "ch-uuid", START, "Tatort");
        when(connection.getEpgList()).thenReturn(new ArrayList<>(List.of(prefixMatch, exactMatch)));
        manager.loadEpgList();

        assertSame(exactMatch, manager.getEpgProgram(programOnChannel("Tatort", START_MIN, 0)));
    }
}
