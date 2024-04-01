package tvhbrowser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import devplugin.Program;

import org.json.JSONArray;
import tvhbrowser.Timer;
import tvhbrowser.TvHBrowser;
import tvhbrowser.TVHeadendConnection;
import tvhbrowser.ChannelManager;
import tvhbrowser.EpgManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static devplugin.Plugin.getPluginManager;
import devplugin.Date;

public class TimerManager {
  private TvHBrowser tvhbrowser;
  private TVHeadendConnection connection;
  private EpgManager epgManager;
  private Map<String, Timer> timerMap;
  private ChannelManager channelManager;
  private Map<String, String> timerToProgramMap;

  private Map<Date, HashMap<String, ArrayList<String>>> timerDateMap;

  public TimerManager(TvHBrowser tvhbrowser, TVHeadendConnection connection, ChannelManager channelManager, EpgManager epgManager) {
    this.tvhbrowser = tvhbrowser;
    this.timerDateMap = new HashMap<>();
    this.timerToProgramMap = new HashMap<>();
    this.connection = connection;
    this.timerMap = new HashMap<>();
    this.channelManager = channelManager;
    this.epgManager = epgManager;
  }

  public void loadTimer() {
    timerMap.clear();
    timerDateMap.clear();
    List<Timer> timers = connection.getUpcomingTimerList();

    timers.sort(Comparator.comparing(Timer::getDate));
    for (Timer timer : timers) {
      timer.extractEpisodeInfo();
      timerMap.put(timer.getUuid(), timer);
      Date timerDate = timer.getDate();
      String channeID = timer.getChannel();
      String value = timer.getUuid();

      if (!timerDateMap.containsKey(timerDate)) {
        timerDateMap.put(timerDate, new HashMap<String, ArrayList<String>>());
        tvhbrowser.writeLog("--- Datum " + timerDate.getShortDayLongMonthString() + " hinzugefügt");
      }

      Map<String, ArrayList<String>> innerMap = timerDateMap.get(timerDate);
      if (!innerMap.containsKey(channeID)) {
        innerMap.put(channeID, new ArrayList<>());
      }

      if (innerMap.get(channeID).contains(value)) {
        tvhbrowser.writeLog("Timer " + value + " bereits hinzugefügt");
      } else {
        innerMap.get(channeID).add(value);
        tvhbrowser.writeLog("Kanal " + channeID + " - Timer " + value + " hinzugefüg");
      }
    }
  }

  public void mapTimerToProgram() {
    HashMap<String, String> oldMap = new HashMap<String, String>(timerToProgramMap);
    timerToProgramMap.clear();

    for (Map.Entry<Date, HashMap<String, ArrayList<String>>> entry : timerDateMap.entrySet()) {
      Date date = entry.getKey();
      Map<String, ArrayList<String>> innerMap = entry.getValue();
      for (Map.Entry<String, ArrayList<String>> innerEntry : innerMap.entrySet()) {
        String channelKey = innerEntry.getKey();
        devplugin.Channel channel = this.channelManager.getTVBChannelbyTVHChannelKey(channelKey);
        if (channel == null) {
          tvhbrowser.writeLog("Channel " + innerEntry.getValue() + " not mapped in settings. Skipping...");
          continue;
        }
        processTimers(oldMap, innerEntry.getValue(), date, channel);
      }
    }

    Map<String, String> diffMap = new HashMap<>();
    // Find keys present only in map1
    for (Map.Entry<String, String> entry : oldMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (!timerToProgramMap.containsKey(key)) {
        diffMap.put(key, value);
      }
    }

    tvhbrowser.writeLog("DiffList: " + diffMap);
    diffMap.forEach(this::handleOldTimer);

  }

  private void processTimers(HashMap<String, String> Map, ArrayList<String> timerIds, Date date,
      devplugin.Channel channel) {

    for (String timerId : timerIds) {

      if (Map.containsKey(timerId)) {
        String programId = Map.get(timerId);
        handleExistingTimer(timerId, programId);
      } else {
        handleNewTimer(timerId, date, channel);
      }
    }
  }

  private void handleOldTimer(String timerId, String programId) {
    Program program = getPluginManager().getProgram(programId);
    tvhbrowser.writeLog("Timer " + timerId + " gelöscht.");
    if (program != null) {
      tvhbrowser.unmarkProgram(program);
    }

    // timerToProgramMap.remove(timerId);
  }

  private void handleExistingTimer(String timerId, String programId) {
    timerToProgramMap.put(timerId, programId);
    tvhbrowser.writeLog("Timer " + timerId + " bereits gemappt");
  }

  private void handleNewTimer(String timerId, Date date, devplugin.Channel channel) {
    Timer timer = timerMap.get(timerId);
    tvhbrowser.writeLog("----------------------------");
    tvhbrowser.writeLog(
        "----" + timer.getTitles() + " ID:" + timerId + " am " + date.getShortDayLongMonthString() + " im Kanal "
            + channel.getName() + " suchen....");
    tvhbrowser.writeLog("Timer Starttime " + timer.getStartDateTime());

    Iterator<Program> programsOfOneDay = getPluginManager().getChannelDayProgram(date, channel);

    boolean isMapped = false; // Flag to check if any mapping was done

    if (programsOfOneDay == null) {
      tvhbrowser
          .writeLog("No programs found for " + date.getShortDayLongMonthString() + " in channel " + channel.getName());
      return;
    }

    while (programsOfOneDay.hasNext()) {
      Program program = programsOfOneDay.next();
      // tvhbrowser.writeLog("Date " + program.getDateString());
      // tvhbrowser.writeLog("Program Starttime " + program.getStartTime());
      // tvhbrowser.writeLog("Title " + program.getTitle());
      // tvhbrowser.writeLog("ShortInfo " + program.getShortInfo());
      // tvhbrowser.writeLog("Descr. " + program.getDescription());
      // tvhbrowser.writeLog("Episode " +
      // program.getTextField(devplugin.ProgramFieldType.EPISODE_TYPE));
      // tvhbrowser.writeLog("ADDInfo " +
      // program.getTextField(devplugin.ProgramFieldType.ADDITIONAL_INFORMATION_TYPE));
      // tvhbrowser.writeLog("OrgTitle " +
      // program.getTextField(devplugin.ProgramFieldType.ORIGINAL_TITLE_TYPE));
      // tvhbrowser.writeLog("Series " +
      // program.getTextField(devplugin.ProgramFieldType.SERIES_TYPE));

      if (timer.compare(program)) {
        timerToProgramMap.put(timerId, program.getUniqueID());
        this.tvhbrowser.markProgram(program);
        program.validateMarking();
        // tvhbrowser.showInfo(program.getInfo() + " marked");
        tvhbrowser.writeLog(program.getTitle() + " matched ");
        isMapped = true;
        break;
      } else {
        tvhbrowser.writeLog("not match");
      }
    }

    if (!isMapped) {
      tvhbrowser.writeLog("No mapping could be done for timer " + timerId);
    }
  }

  public Map<String, Timer> getTimerMap() {
    return timerMap;
  }

  public void updateTimer() {
    this.loadTimer();
    this.mapTimerToProgram();
  }

  public void createTimer(Program program) {
    EpgProgram  epgProgram = this.epgManager.getEpgProgram(program);

    if (epgProgram == null) {
      tvhbrowser.showError("No EPG data found for program " + program.getTitle());
      return;
    }

    this.connection.createTimerWithEvent(epgProgram.getEventId());
    this.updateTimer();
  }

  public void deleteTimer(Program program) {
    String programId = program.getUniqueID();
    String timerId = timerToProgramMap.entrySet().stream().filter(entry -> entry.getValue().equals(programId))
        .findFirst()
        .get().getKey();
    tvhbrowser.showInfo("Delete timer " + timerId);
    connection.deleteTimer(timerId);
    this.updateTimer();
  }

  public void handleTvDataUpdateFinished() {

    Map<String, String> oldList = timerToProgramMap;
    timerToProgramMap.clear();

    for (Map.Entry<String, String> map : oldList.entrySet()) {
      String timerId = map.getKey();
      String programId = map.getValue();
      Program program = getPluginManager().getProgram(programId);
      if (program == null || program.getProgramState() == Program.STATE_WAS_DELETED) {

      } else if (program.getProgramState() == Program.STATE_WAS_UPDATED) {

        timerToProgramMap.put(timerId, programId);
      } else {

        timerToProgramMap.put(timerId, programId);
      }
    }
  }

      /**
     * Writes the channel mapping data to an ObjectOutputStream.
     *
     * @param out   The ObjectOutputStream to write to.
     * @throws IOException   If an I/O error occurs while writing the data.
     */
    public void writeData(ObjectOutputStream out) throws IOException {
        out.writeInt(timerToProgramMap.size());
        for (Map.Entry<String, String> entry : timerToProgramMap.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeUTF(entry.getValue());
        }
    }

    /**
     * Reads the channel mapping data from an ObjectInputStream.
     *
     * @param in   The ObjectInputStream to read from.
     * @throws IOException   If an I/O error occurs while reading the data.
     */
    public void readData(ObjectInputStream in) throws IOException {
        if (timerMap.size() == 0) {
            this.getTimerMap();
        }

        String timerId;
        String programId;
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            timerId = in.readUTF();
            programId = in.readUTF();
            handleExistingTimer(timerId, programId);
        }
    }
}
