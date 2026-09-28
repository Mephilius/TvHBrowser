package tvhbrowser;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static tvhbrowser.TestSupport.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import devplugin.Channel;
import devplugin.Date;
import devplugin.Plugin;
import devplugin.PluginManager;
import devplugin.Program;

public class TimerManagerTest {

    private TvHBrowser browser;
    private TVHeadendConnection connection;
    private ChannelManager channels;
    private EpgManager epg;
    private TimerManager manager;
    private PluginManager pm;
    private MockedStatic<Plugin> pluginStatic;

    private static final long START = epoch(2026, 9, 28, 20, 15);
    private static final int START_MIN = 20 * 60 + 15;

    @Before
    public void setUp() {
        browser = mock(TvHBrowser.class);
        connection = mock(TVHeadendConnection.class);
        channels = mock(ChannelManager.class);
        epg = mock(EpgManager.class);
        manager = new TimerManager(browser, connection, channels, epg);

        pm = mock(PluginManager.class);
        pluginStatic = mockStatic(Plugin.class);
        pluginStatic.when(Plugin::getPluginManager).thenReturn(pm);
    }

    @After
    public void tearDown() {
        pluginStatic.close();
    }

    private Program programWithState(String id, int state) {
        Program p = mock(Program.class);
        when(p.getUniqueID()).thenReturn(id);
        when(p.getProgramState()).thenReturn(state);
        when(pm.getProgram(id)).thenReturn(p);
        return p;
    }

    private void seedMapping(String timerId, String programId) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeInt(1);
            out.writeUTF(timerId);
            out.writeUTF(programId);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            manager.readData(in);
        }
    }

    @Test
    public void loadTimerFillsTimerMapSortedAndDeduplicated() {
        Timer a = timer("a", "ch1", START, "ger", "Tatort");
        Timer b = timer("b", "ch1", START + 3600, "ger", "Heute");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(b, a)));

        manager.loadTimer();

        assertEquals(2, manager.getTimerMap().size());
        assertSame(a, manager.getTimerMap().get("a"));
        assertEquals(List.of(a, b), manager.getUpcomingTimers());
    }

    @Test
    public void loadTimerClearsPreviousState() {
        when(connection.getUpcomingTimerList())
                .thenReturn(new ArrayList<>(List.of(timer("a", "c", START, "ger", "X"))))
                .thenReturn(new ArrayList<>());
        manager.loadTimer();
        manager.loadTimer();
        assertTrue(manager.getTimerMap().isEmpty());
    }

    @Test
    public void loadTimerExtractsEpisodeInfo() {
        Timer a = timer("a", "c", START, "ger", "Serie (1/2)");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(a)));
        manager.loadTimer();
        assertEquals(1, a.getEpisodeNumber());
    }

    @Test
    public void mapTimerToProgramMarksMatchingProgram() {
        Timer a = timer("a", "ch1", START, "ger", "Tatort");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(a)));
        manager.loadTimer();

        Channel channel = mock(Channel.class);
        when(channel.getName()).thenReturn("ARD");
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(channel);

        Program match = programWithState("prog-1", Program.STATE_IS_VALID);
        when(match.getTitle()).thenReturn("Tatort");
        when(match.getStartTime()).thenReturn(START_MIN);
        when(pm.getChannelDayProgram(any(Date.class), eq(channel)))
                .thenAnswer(i -> List.of(match).iterator());

        manager.mapTimerToProgram();

        verify(browser).markProgram(match);
        assertSame(match, manager.getProgramForTimer("a"));
    }

    @Test
    public void mapTimerToProgramSkipsUnmappedChannels() {
        when(connection.getUpcomingTimerList())
                .thenReturn(new ArrayList<>(List.of(timer("a", "ch1", START, "ger", "Tatort"))));
        manager.loadTimer();
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(null);

        manager.mapTimerToProgram();

        verify(browser, never()).markProgram(any());
        assertNull(manager.getProgramForTimer("a"));
    }

    @Test
    public void mapTimerToProgramUnmarksProgramsOfRemovedTimers() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("gone", "prog-1");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());
        manager.loadTimer();

        manager.mapTimerToProgram();

        verify(browser).unmarkProgram(p);
    }

    @Test
    public void tvDataUpdateKeepsMappingForExistingPrograms() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("a", "prog-1");

        manager.handleTvDataUpdateFinished();

        assertEquals("prog-1", programIdOf("a"));
        assertSame(p, manager.getProgramForTimer("a"));
    }

    @Test
    public void tvDataUpdateKeepsMappingForUpdatedPrograms() throws Exception {
        programWithState("prog-1", Program.STATE_WAS_UPDATED);
        seedMapping("a", "prog-1");

        manager.handleTvDataUpdateFinished();

        assertEquals("prog-1", programIdOf("a"));
    }

    @Test
    public void tvDataUpdateDropsMappingForDeletedPrograms() throws Exception {
        programWithState("prog-1", Program.STATE_WAS_DELETED);
        seedMapping("a", "prog-1");

        manager.handleTvDataUpdateFinished();

        assertNull(programIdOf("a"));
    }

    @Test
    public void tvDataUpdateDropsMappingForUnknownPrograms() throws Exception {
        when(pm.getProgram("missing")).thenReturn(null);
        seedMapping("a", "missing");

        manager.handleTvDataUpdateFinished();

        assertNull(programIdOf("a"));
    }

    private String programIdOf(String timerId) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            manager.writeData(out);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                String t = in.readUTF();
                String p = in.readUTF();
                if (t.equals(timerId)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Test
    public void getProgramForTimerReturnsNullForDeletedProgram() throws Exception {
        programWithState("prog-1", Program.STATE_WAS_DELETED);
        seedMapping("a", "prog-1");
        assertNull(manager.getProgramForTimer("a"));
    }

    @Test
    public void getProgramForTimerReturnsNullWhenUnmapped() {
        assertNull(manager.getProgramForTimer("nope"));
    }

    @Test
    public void createTimerCallsConnectionWithEventIdAndRefreshes() {
        Program p = mock(Program.class);
        EpgProgram e = new EpgProgram();
        e.setEventId(77);
        when(epg.getEpgProgram(p)).thenReturn(e);
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());

        manager.createTimer(p);

        verify(connection).createTimerWithEvent(77);
        verify(browser).refreshProgramTree();
    }

    @Test
    public void createTimerShowsErrorWithoutEpgData() {
        Program p = mock(Program.class);
        when(p.getTitle()).thenReturn("Tatort");
        when(epg.getEpgProgram(p)).thenReturn(null);

        manager.createTimer(p);

        verify(browser).showError(contains("Tatort"));
        verify(connection, never()).createTimerWithEvent(any());
    }

    @Test
    public void deleteUsesMappedTimerId() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("timer-1", "prog-1");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());
        manager.deleteTimer(p);
        verify(connection).deleteTimer("timer-1");
    }

    @Test
    public void enableUsesMappedTimerId() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("timer-1", "prog-1");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());
        manager.enableTimer(p);
        verify(connection).enableTimer("timer-1");
    }

    @Test
    public void disableUsesMappedTimerId() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("timer-1", "prog-1");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());
        manager.disableTimer(p);
        verify(connection).disableTimer("timer-1");
    }

    @Test
    public void actionsWithoutMappingShowErrorAndSkipConnection() {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);

        manager.deleteTimer(p);
        manager.enableTimer(p);
        manager.disableTimer(p);

        verify(browser, times(3)).showError(anyString());
        verify(connection, never()).deleteTimer(any());
        verify(connection, never()).enableTimer(any());
        verify(connection, never()).disableTimer(any());
    }

    @Test
    public void isTimerEnabledReflectsTimerState() throws Exception {
        Timer t = timer("timer-1", "c", START, "ger", "X");
        t.setEnabled(false);
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(t)));
        manager.loadTimer();
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("timer-1", "prog-1");

        assertFalse(manager.isTimerEnabled(p));
        t.setEnabled(true);
        assertTrue(manager.isTimerEnabled(p));
    }

    @Test
    public void isTimerEnabledDefaultsToTrueWithoutTimer() {
        assertTrue(manager.isTimerEnabled(programWithState("prog-1", Program.STATE_IS_VALID)));
    }

    @Test
    public void finishedTimersAreSortedNewestFirst() {
        Timer older = timer("o", "c", START, "ger", "A");
        Timer newer = timer("n", "c", START + 86400 * 2, "ger", "B");
        when(connection.getFinishedTimerList()).thenReturn(new ArrayList<>(List.of(older, newer)));
        assertEquals(List.of(newer, older), manager.getFinishedTimers());
    }

    @Test
    public void finishedTimersEmptyWhenConnectionReturnsNull() {
        when(connection.getFinishedTimerList()).thenReturn(null);
        assertTrue(manager.getFinishedTimers().isEmpty());
    }

    @Test
    public void upcomingTimerProgramsFollowMapping() throws Exception {
        Timer t = timer("timer-1", "c", START, "ger", "X");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(t)));
        manager.loadTimer();
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("timer-1", "prog-1");

        assertEquals(List.of(p), manager.getUpcomingTimerPrograms());
    }

    @Test
    public void upcomingTimerProgramsEmptyWithoutTimers() {
        assertTrue(manager.getUpcomingTimerPrograms().isEmpty());
    }

    @Test
    public void updateTimerReloadsRemapsAndRefreshesTree() {
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());
        manager.updateTimer();
        verify(connection).getUpcomingTimerList();
        verify(browser).refreshProgramTree();
    }
}
