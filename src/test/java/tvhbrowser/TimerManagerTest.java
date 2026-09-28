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
        manager = newManager();
        when(epg.isReady()).thenReturn(true);

        pm = mock(PluginManager.class);
        pluginStatic = mockStatic(Plugin.class);
        pluginStatic.when(Plugin::getPluginManager).thenReturn(pm);
    }

    @After
    public void tearDown() {
        pluginStatic.close();
    }

    private TimerManager newManager() {
        return new TimerManager(browser, connection, channels, epg, Runnable::run, Runnable::run);
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
    public void mapTimerToProgramFallsBackToNearestTextMatch() {
        Timer a = timer("a", "ch1", epoch(2026, 9, 28, 23, 35), "ger", "Stuttgart 21");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(a)));
        manager.loadTimer();

        Channel channel = mock(Channel.class);
        when(channel.getName()).thenReturn("ARD");
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(channel);

        Program late = programWithState("prog-late", Program.STATE_IS_VALID);
        when(late.getTitle()).thenReturn("Stuttgart 21");
        when(late.getStartTime()).thenReturn(23 * 60 + 50);
        when(pm.getChannelDayProgram(any(Date.class), eq(channel)))
                .thenAnswer(i -> List.of(late).iterator());

        manager.mapTimerToProgram();

        verify(browser).markProgram(late);
        assertSame(late, manager.getProgramForTimer("a"));
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
    public void createTimerAsksUserWithSimilarEntryAndUsesChoice() {
        Program p = mock(Program.class);
        when(p.getTitle()).thenReturn("Tatort");
        EpgProgram similar = new EpgProgram();
        similar.setEventId(11);
        when(epg.getEpgProgram(p)).thenReturn(null);
        when(epg.getSimilarEpgProgram(p)).thenReturn(similar);
        when(browser.chooseEpgProgram(anyString(), eq(p), eq(List.of(similar)))).thenReturn(similar);
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());

        manager.createTimer(p);

        verify(connection).createTimerWithEvent(11);
    }

    @Test
    public void createTimerOffersSurroundingEntriesWhenNothingSimilar() {
        Program p = mock(Program.class);
        when(p.getTitle()).thenReturn("Tatort");
        EpgProgram a = new EpgProgram();
        a.setEventId(21);
        EpgProgram b = new EpgProgram();
        b.setEventId(22);
        when(epg.getEpgProgram(p)).thenReturn(null);
        when(epg.getSimilarEpgProgram(p)).thenReturn(null);
        when(epg.getSurroundingEpgPrograms(p)).thenReturn(List.of(a, b));
        when(browser.chooseEpgProgram(anyString(), eq(p), eq(List.of(a, b)))).thenReturn(b);
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());

        manager.createTimer(p);

        verify(connection).createTimerWithEvent(22);
    }

    @Test
    public void createTimerDoesNothingWhenUserCancels() {
        Program p = mock(Program.class);
        EpgProgram a = new EpgProgram();
        when(epg.getSurroundingEpgPrograms(p)).thenReturn(List.of(a));
        when(browser.chooseEpgProgram(anyString(), any(), any())).thenReturn(null);

        manager.createTimer(p);

        verify(connection, never()).createTimerWithEvent(any());
        verify(browser, never()).showError(anyString());
    }

    @Test
    public void createTimerShowsErrorWhenNoCandidatesExist() {
        Program p = mock(Program.class);
        when(p.getTitle()).thenReturn("Tatort");
        when(epg.getSurroundingEpgPrograms(p)).thenReturn(new ArrayList<>());

        manager.createTimer(p);

        verify(browser).showError(contains("Tatort"));
        verify(browser, never()).chooseEpgProgram(anyString(), any(), any());
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
        manager.updateTimer();
        assertEquals(List.of(newer, older), manager.getFinishedTimers());
    }

    @Test
    public void finishedTimersEmptyWhenConnectionReturnsNull() {
        when(connection.getFinishedTimerList()).thenReturn(null);
        manager.updateTimer();
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

    @Test
    public void createTimerShowsInfoWhileEpgIsLoading() {
        when(epg.isReady()).thenReturn(false);
        Program p = mock(Program.class);

        manager.createTimer(p);

        verify(browser).showInfo(contains("geladen"));
        verify(epg, never()).getEpgProgram(any(Program.class));
        verify(connection, never()).createTimerWithEvent(any());
    }

    private Program chooseSurroundingAndCreate(Timer created, Channel channel, Program p) {
        EpgProgram chosen = new EpgProgram();
        chosen.setEventId(22);
        when(epg.getEpgProgram(p)).thenReturn(null);
        when(epg.getSimilarEpgProgram(p)).thenReturn(null);
        when(epg.getSurroundingEpgPrograms(p)).thenReturn(List.of(chosen));
        when(browser.chooseEpgProgram(anyString(), eq(p), any())).thenReturn(chosen);
        when(connection.createTimerWithEvent(22)).thenReturn(created.getUuid());
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(created)));
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(channel);
        when(pm.getChannelDayProgram(any(Date.class), eq(channel))).thenAnswer(i -> new ArrayList<Program>().iterator());
        manager.createTimer(p);
        return p;
    }

    @Test
    public void userChosenTimerIsMappedEvenWithDifferentTitle() {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        Timer created = timer("new-uuid", "ch1", START, "ger", "Voellig anderer Titel");

        chooseSurroundingAndCreate(created, mock(Channel.class), p);

        verify(browser).markProgram(p);
        assertSame(p, manager.getProgramForTimer("new-uuid"));
    }

    @Test
    public void userChosenMappingSurvivesRestart() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        Timer created = timer("new-uuid", "ch1", START, "ger", "Voellig anderer Titel");
        Channel channel = mock(Channel.class);
        chooseSurroundingAndCreate(created, channel, p);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            manager.writeManualMappings(out);
        }

        TimerManager restarted = newManager();
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restarted.readManualMappings(in);
        }
        restarted.loadTimer();
        restarted.mapTimerToProgram();

        assertSame(p, restarted.getProgramForTimer("new-uuid"));
    }

    @Test
    public void manualMappingIsDroppedWhenTimerDisappears() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        Timer created = timer("new-uuid", "ch1", START, "ger", "Voellig anderer Titel");
        chooseSurroundingAndCreate(created, mock(Channel.class), p);

        Timer other = timer("other", "ch1", START + 3600, "ger", "Anderes");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(other)));
        manager.loadTimer();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            manager.writeManualMappings(out);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(0, in.readInt());
        }
    }

    @Test
    public void automaticallyFoundTimerIsNotStoredAsManualMapping() throws Exception {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        EpgProgram exact = new EpgProgram();
        exact.setEventId(5);
        when(epg.getEpgProgram(p)).thenReturn(exact);
        when(connection.createTimerWithEvent(5)).thenReturn("uuid-5");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>());

        manager.createTimer(p);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            manager.writeManualMappings(out);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(0, in.readInt());
        }
    }

    @Test
    public void updateTimerFetchesInBackgroundAndAppliesOnUiThread() {
        List<Runnable> backgroundQueue = new ArrayList<>();
        List<Runnable> uiQueue = new ArrayList<>();
        TimerManager async = new TimerManager(browser, connection, channels, epg, backgroundQueue::add,
                uiQueue::add);
        when(connection.getUpcomingTimerList())
                .thenReturn(new ArrayList<>(List.of(timer("a", "c", START, "ger", "X"))));

        async.updateTimer();
        async.updateTimer();

        assertEquals(1, backgroundQueue.size());
        verify(connection, never()).getUpcomingTimerList();

        backgroundQueue.get(0).run();
        verify(connection).getUpcomingTimerList();
        assertTrue(async.getTimerMap().isEmpty());
        assertEquals(1, uiQueue.size());

        uiQueue.get(0).run();
        assertEquals(1, async.getTimerMap().size());
        verify(browser).refreshProgramTree();
    }

    @Test
    public void failedFetchKeepsExistingTimersAndMappings() throws Exception {
        Timer t = timer("timer-1", "c", START, "ger", "X");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(t))).thenReturn(null);
        manager.updateTimer();
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        seedMapping("timer-1", "prog-1");

        manager.updateTimer();

        assertEquals(1, manager.getTimerMap().size());
        assertSame(p, manager.getProgramForTimer("timer-1"));
        verify(browser, never()).unmarkProgram(any());
    }

    @Test
    public void unmappedTimerIsOnlySearchedAgainAfterRetryInterval() {
        long[] now = { 1_000_000L };
        manager.clock = () -> now[0];
        Timer t = timer("a", "ch1", START, "ger", "Nirgends");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(t)));
        Channel channel = mock(Channel.class);
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(channel);
        when(pm.getChannelDayProgram(any(Date.class), eq(channel)))
                .thenAnswer(i -> new ArrayList<Program>().iterator());

        manager.updateTimer();
        manager.updateTimer();
        verify(pm, times(1)).getChannelDayProgram(any(Date.class), eq(channel));

        now[0] += TimerManager.UNMAPPED_RETRY_MILLIS + 1;
        manager.updateTimer();
        verify(pm, times(2)).getChannelDayProgram(any(Date.class), eq(channel));
    }

    @Test
    public void tvDataUpdateResetsUnmappedRetryThrottle() {
        Timer t = timer("a", "ch1", START, "ger", "Nirgends");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(t)));
        Channel channel = mock(Channel.class);
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(channel);
        when(pm.getChannelDayProgram(any(Date.class), eq(channel)))
                .thenAnswer(i -> new ArrayList<Program>().iterator());

        manager.updateTimer();
        manager.handleTvDataUpdateFinished();
        manager.updateTimer();

        verify(pm, times(2)).getChannelDayProgram(any(Date.class), eq(channel));
    }

    @Test
    public void programStaysMarkedWhileAnotherTimerStillMapsToIt() {
        Program p = programWithState("prog-1", Program.STATE_IS_VALID);
        when(p.getTitle()).thenReturn("Tatort");
        when(p.getStartTime()).thenReturn(START_MIN);
        Channel channel = mock(Channel.class);
        when(channels.getTVBChannelbyTVHChannelKey("ch1")).thenReturn(channel);
        when(pm.getChannelDayProgram(any(Date.class), eq(channel))).thenAnswer(i -> List.of(p).iterator());

        Timer first = timer("first", "ch1", START, "ger", "Tatort");
        Timer second = timer("second", "ch1", START, "ger", "Tatort");
        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(first, second)));
        manager.updateTimer();

        when(connection.getUpcomingTimerList()).thenReturn(new ArrayList<>(List.of(second)));
        manager.updateTimer();

        verify(browser, never()).unmarkProgram(p);
        assertSame(p, manager.getProgramForTimer("second"));
    }

    @Test
    public void shutdownWithoutOwnedExecutorIsSafe() {
        manager.shutdown();
    }
}
