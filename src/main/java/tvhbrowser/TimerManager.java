package tvhbrowser;

import devplugin.Date;
import devplugin.Marker;
import devplugin.Program;

import static devplugin.Plugin.getPluginManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

import javax.swing.SwingUtilities;

/**
 * Keeps the Tvheadend timers and their mapping to TV-Browser programs.
 * Network access runs on a background thread, every change of the state runs
 * on the Swing thread.
 */
public class TimerManager {
  static final long UNMAPPED_RETRY_MILLIS = 10 * 60 * 1000L;

  private final TvHBrowser tvhbrowser;
  private final TVHeadendConnection connection;
  private final EpgManager epgManager;
  private final ChannelManager channelManager;
  private final Executor background;
  private final Executor ui;
  private final ExecutorService ownedBackground;
  private final AtomicBoolean refreshPending = new AtomicBoolean();

  private final Map<String, Timer> timerMap = new HashMap<>();
  private final Map<Date, HashMap<String, ArrayList<String>>> timerDateMap = new HashMap<>();
  private final Map<String, String> timerToProgramMap = new ConcurrentHashMap<>();
  private final Map<String, String> manualMappings = new ConcurrentHashMap<>();
  private final Map<String, Long> unmappedSince = new HashMap<>();
  private final Set<String> unmappedLogged = new HashSet<>();
  private List<Timer> finishedTimers = new ArrayList<>();

  LongSupplier clock = System::currentTimeMillis;

  public TimerManager(TvHBrowser tvhbrowser, TVHeadendConnection connection, ChannelManager channelManager,
      EpgManager epgManager) {
    this(tvhbrowser, connection, channelManager, epgManager, null, SwingUtilities::invokeLater);
  }

  TimerManager(TvHBrowser tvhbrowser, TVHeadendConnection connection, ChannelManager channelManager,
      EpgManager epgManager, Executor background, Executor ui) {
    this.tvhbrowser = tvhbrowser;
    this.connection = connection;
    this.channelManager = channelManager;
    this.epgManager = epgManager;
    this.ui = ui;
    if (background == null) {
      this.ownedBackground = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tvh-timer-io");
        thread.setDaemon(true);
        return thread;
      });
      this.background = ownedBackground;
    } else {
      this.ownedBackground = null;
      this.background = background;
    }
  }

  public void shutdown() {
    if (ownedBackground != null) {
      ownedBackground.shutdownNow();
    }
  }

  /** Fetches the timers in the background and applies them on the Swing thread. */
  public void updateTimer() {
    if (refreshPending.compareAndSet(false, true)) {
      background.execute(this::refresh);
    }
  }

  private void refresh() {
    refreshPending.set(false);
    List<Timer> upcoming = connection.getUpcomingTimerList();
    List<Timer> finished = connection.getFinishedTimerList();
    ui.execute(() -> apply(upcoming, finished));
  }

  private void apply(List<Timer> upcoming, List<Timer> finished) {
    if (finished != null) {
      List<Timer> sorted = new ArrayList<>(finished);
      sorted.sort(Comparator.comparing(Timer::getDate).thenComparing(Timer::getStartDateTime).reversed());
      finishedTimers = sorted;
    }

    if (upcoming != null) {
      loadTimer(upcoming);
      mapTimerToProgram();
    }

    if (upcoming != null || finished != null) {
      tvhbrowser.refreshProgramTree();
    }
  }

  public void loadTimer() {
    loadTimer(connection.getUpcomingTimerList());
  }

  private void loadTimer(List<Timer> loaded) {
    if (loaded == null) {
      return;
    }

    List<Timer> timers = new ArrayList<>(loaded);
    timerMap.clear();
    timerDateMap.clear();

    timers.sort(Comparator.comparing(Timer::getDate));
    if (!timers.isEmpty()) {
      Set<String> currentIds = new HashSet<>();
      for (Timer timer : timers) {
        currentIds.add(timer.getUuid());
      }
      manualMappings.keySet().retainAll(currentIds);
      unmappedSince.keySet().retainAll(currentIds);
      unmappedLogged.retainAll(currentIds);
    }
    for (Timer timer : timers) {
      timer.extractEpisodeInfo();
      timerMap.put(timer.getUuid(), timer);
      List<String> ids = timerDateMap
          .computeIfAbsent(timer.getDate(), d -> new HashMap<>())
          .computeIfAbsent(timer.getChannel(), c -> new ArrayList<>());
      if (!ids.contains(timer.getUuid())) {
        ids.add(timer.getUuid());
      }
    }
  }

  public void mapTimerToProgram() {
    HashMap<String, String> oldMap = new HashMap<>(timerToProgramMap);
    timerToProgramMap.clear();

    for (Map.Entry<Date, HashMap<String, ArrayList<String>>> entry : timerDateMap.entrySet()) {
      Date date = entry.getKey();
      for (Map.Entry<String, ArrayList<String>> innerEntry : entry.getValue().entrySet()) {
        devplugin.Channel channel = this.channelManager.getTVBChannelbyTVHChannelKey(innerEntry.getKey());
        if (channel == null) {
          continue;
        }
        processTimers(oldMap, innerEntry.getValue(), date, channel);
      }
    }

    Map<String, String> removed = new HashMap<>();
    for (Map.Entry<String, String> entry : oldMap.entrySet()) {
      if (!timerToProgramMap.containsKey(entry.getKey())) {
        removed.put(entry.getKey(), entry.getValue());
      }
    }

    if (!removed.isEmpty()) {
      tvhbrowser.writeLog("Removed timers: " + removed);
    }
    removed.forEach(this::handleOldTimer);
  }

  private void processTimers(Map<String, String> oldMap, List<String> timerIds, Date date,
      devplugin.Channel channel) {
    for (String timerId : timerIds) {
      String programId = oldMap.get(timerId);
      if (programId != null) {
        timerToProgramMap.put(timerId, programId);
      } else {
        handleNewTimer(timerId, date, channel);
      }
    }
  }

  private void handleOldTimer(String timerId, String programId) {
    if (timerToProgramMap.containsValue(programId)) {
      return;
    }

    Program program = getPluginManager().getProgram(programId);
    if (program != null) {
      tvhbrowser.unmarkProgram(program);
    }
  }

  private void mapTo(String timerId, Program program) {
    timerToProgramMap.put(timerId, program.getUniqueID());
    tvhbrowser.markProgram(program);
    program.validateMarking();
    unmappedSince.remove(timerId);
  }

  private boolean mapManually(String timerId) {
    String programId = manualMappings.get(timerId);
    if (programId == null) {
      return false;
    }

    Program program = getPluginManager().getProgram(programId);
    if (program == null || program.getProgramState() == Program.STATE_WAS_DELETED) {
      return false;
    }

    mapTo(timerId, program);
    tvhbrowser.writeLog(program.getTitle() + " mapped to timer " + timerId + " by user choice");
    return true;
  }

  private boolean recentlyUnmapped(String timerId) {
    Long since = unmappedSince.get(timerId);
    return since != null && clock.getAsLong() - since < UNMAPPED_RETRY_MILLIS;
  }

  private void handleNewTimer(String timerId, Date date, devplugin.Channel channel) {
    Timer timer = timerMap.get(timerId);

    if (mapManually(timerId) || recentlyUnmapped(timerId)) {
      return;
    }

    Iterator<Program> programsOfOneDay = getPluginManager().getChannelDayProgram(date, channel);
    if (programsOfOneDay == null) {
      return;
    }

    // Pick the best-scoring candidate instead of the first one that matches, and prefer a
    // match within the strict time window over a wider, fallback one even if its text score
    // happens to be lower.
    Program bestStrict = null;
    long bestStrictScore = TimerEpgInterface.NO_MATCH;
    Program bestFallback = null;
    long bestFallbackScore = TimerEpgInterface.NO_MATCH;

    while (programsOfOneDay.hasNext()) {
      Program program = programsOfOneDay.next();
      long score = timer.matchScore(program, TimerEpgInterface.FALLBACK_TOLERANCE_SECONDS);
      if (score == TimerEpgInterface.NO_MATCH) {
        continue;
      }

      if (score > bestFallbackScore) {
        bestFallback = program;
        bestFallbackScore = score;
      }
      if (timer.timeDistanceSeconds(program) <= TimerEpgInterface.STRICT_TOLERANCE_SECONDS
          && score > bestStrictScore) {
        bestStrict = program;
        bestStrictScore = score;
      }
    }

    if (bestStrict != null) {
      mapTo(timerId, bestStrict);
      tvhbrowser.writeLog(bestStrict.getTitle() + " matched ");
      return;
    }

    if (bestFallback != null) {
      long distance = timer.timeDistanceSeconds(bestFallback);
      mapTo(timerId, bestFallback);
      tvhbrowser.writeLog(bestFallback.getTitle() + " matched with time offset of " + distance / 60 + " min");
      return;
    }

    unmappedSince.put(timerId, clock.getAsLong());
    if (unmappedLogged.add(timerId)) {
      tvhbrowser.writeLog("No mapping could be done for timer " + timerId + " " + timer.getTitles() + " at "
          + timer.getStartDateTime() + " texts: " + timer.getTimerTextToCompare());
    }
  }

  public Map<String, Timer> getTimerMap() {
    return timerMap;
  }

  public List<Program> getUpcomingTimerPrograms() {
    List<Timer> sortedTimers = new ArrayList<>(timerMap.values());
    sortedTimers.sort(Comparator.comparing(Timer::getDate));

    Set<Program> programs = new LinkedHashSet<>();
    for (Timer timer : sortedTimers) {
      Program program = getProgramForTimer(timer.getUuid());
      if (program != null) {
        programs.add(program);
      }
    }

    if (programs.isEmpty() && !timerMap.isEmpty()) {
      Date today = Date.getCurrentDate();
      Program[] markedPrograms = getPluginManager().getMarkedPrograms();
      for (Program markedProgram : markedPrograms) {
        if (markedProgram == null || markedProgram.getProgramState() == Program.STATE_WAS_DELETED) {
          continue;
        }
        if (!isMarkedByThisPlugin(markedProgram)) {
          continue;
        }
        if (markedProgram.getDate().compareTo(today) < 0) {
          continue;
        }
        programs.add(markedProgram);
      }
    }

    List<Program> sortedPrograms = new ArrayList<>(programs);
    sortedPrograms.sort(
        Comparator.comparing(Program::getDate)
            .thenComparingInt(Program::getStartTime));

    return sortedPrograms;
  }

  public List<Timer> getUpcomingTimers() {
    List<Timer> sortedTimers = new ArrayList<>(timerMap.values());
    sortedTimers.sort(
        Comparator.comparing(Timer::getDate)
            .thenComparing(Timer::getStartDateTime));
    return sortedTimers;
  }

  public List<Timer> getFinishedTimers() {
    return Collections.unmodifiableList(finishedTimers);
  }

  public Program getProgramForTimer(String timerId) {
    String programId = timerToProgramMap.get(timerId);
    if (programId == null) {
      return null;
    }

    Program program = getPluginManager().getProgram(programId);
    if (program == null || program.getProgramState() == Program.STATE_WAS_DELETED) {
      return null;
    }

    return program;
  }

  private boolean isMarkedByThisPlugin(Program program) {
    for (Marker marker : program.getMarkerArr()) {
      if (marker == this.tvhbrowser) {
        return true;
      }
    }

    return false;
  }

  public void createTimer(Program program) {
    if (!this.epgManager.isReady()) {
      tvhbrowser.showInfo(TvHBrowser.getTranslation("epgLoading",
          "Die EPG-Daten werden noch vom Tvheadend geladen.\nBitte in einigen Sekunden erneut versuchen."));
      return;
    }

    EpgProgram epgProgram = this.epgManager.getEpgProgram(program);
    boolean chosenByUser = false;

    if (epgProgram == null) {
      epgProgram = chooseAlternative(program);
      if (epgProgram == null) {
        return;
      }
      chosenByUser = true;
    }

    int eventId = epgProgram.getEventId();
    boolean manual = chosenByUser;
    String programId = program.getUniqueID();
    background.execute(() -> {
      String timerId = connection.createTimerWithEvent(eventId);
      if (manual && timerId != null) {
        manualMappings.put(timerId, programId);
      }
      refresh();
    });
  }

  private EpgProgram chooseAlternative(Program program) {
    EpgProgram similar = this.epgManager.getSimilarEpgProgram(program);
    if (similar != null) {
      return tvhbrowser.chooseEpgProgram(
          TvHBrowser.getTranslation("chooseSimilar",
              "Die Sendung „{0}“ wurde im Tvheadend mit abweichender Startzeit gefunden.\nSoll dieser Eintrag verwendet werden?",
              program.getTitle()),
          program, Collections.singletonList(similar));
    }

    List<EpgProgram> surrounding = this.epgManager.getSurroundingEpgPrograms(program);
    if (surrounding.isEmpty()) {
      tvhbrowser.showError("No EPG data found for program " + program.getTitle());
      return null;
    }

    return tvhbrowser.chooseEpgProgram(
        TvHBrowser.getTranslation("chooseSurrounding",
            "Die Sendung „{0}“ wurde im Tvheadend nicht gefunden.\nWelche Sendung soll aufgenommen werden?",
            program.getTitle()),
        program, surrounding);
  }

  public void deleteTimer(Program program) {
    String timerId = getTimerIdForProgram(program, "delete");
    if (timerId != null) {
      background.execute(() -> {
        connection.deleteTimer(timerId);
        refresh();
      });
    }
  }

  public void enableTimer(Program program) {
    String timerId = getTimerIdForProgram(program, "enable");
    if (timerId != null) {
      background.execute(() -> {
        connection.enableTimer(timerId);
        refresh();
      });
    }
  }

  public void disableTimer(Program program) {
    String timerId = getTimerIdForProgram(program, "disable");
    if (timerId != null) {
      background.execute(() -> {
        connection.disableTimer(timerId);
        refresh();
      });
    }
  }

  public boolean isTimerEnabled(Program program) {
    String timerId = findTimerIdForProgram(program);
    if (timerId == null) {
      return true;
    }

    Timer timer = timerMap.get(timerId);
    return timer == null || timer.isEnabled();
  }

  private String findTimerIdForProgram(Program program) {
    String programId = program.getUniqueID();
    return timerToProgramMap.entrySet().stream()
        .filter(entry -> entry.getValue().equals(programId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  private String getTimerIdForProgram(Program program, String action) {
    String timerId = findTimerIdForProgram(program);

    if (timerId == null) {
      tvhbrowser.showError(
          "No timer mapping found for program '" + program.getTitle() + "' while trying to " + action + ".");
      tvhbrowser.writeLog("No timer mapping for programId " + program.getUniqueID() + " while trying to " + action);
    }

    return timerId;
  }

  public void handleTvDataUpdateFinished() {
    ui.execute(() -> {
      Map<String, String> oldList = new HashMap<>(timerToProgramMap);
      timerToProgramMap.clear();
      unmappedSince.clear();

      for (Map.Entry<String, String> entry : oldList.entrySet()) {
        Program program = getPluginManager().getProgram(entry.getValue());
        if (program != null && program.getProgramState() != Program.STATE_WAS_DELETED) {
          timerToProgramMap.put(entry.getKey(), entry.getValue());
        }
      }
    });
  }

  public void writeData(ObjectOutputStream out) throws IOException {
    writeMap(out, timerToProgramMap);
  }

  public void readData(ObjectInputStream in) throws IOException {
    readMap(in, timerToProgramMap);
  }

  public void writeManualMappings(ObjectOutputStream out) throws IOException {
    writeMap(out, manualMappings);
  }

  public void readManualMappings(ObjectInputStream in) throws IOException {
    readMap(in, manualMappings);
  }

  private static void writeMap(ObjectOutputStream out, Map<String, String> map) throws IOException {
    Map<String, String> copy = new HashMap<>(map);
    out.writeInt(copy.size());
    for (Map.Entry<String, String> entry : copy.entrySet()) {
      out.writeUTF(entry.getKey());
      out.writeUTF(entry.getValue());
    }
  }

  private static void readMap(ObjectInputStream in, Map<String, String> target) throws IOException {
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      String key = in.readUTF();
      target.put(key, in.readUTF());
    }
  }
}
