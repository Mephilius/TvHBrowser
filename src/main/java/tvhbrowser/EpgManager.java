package tvhbrowser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import devplugin.Date;
import devplugin.Program;

public class EpgManager {

    final private TvHBrowser tvhBrowser;
    final private ChannelManager channelManager;
    final private TVHeadendConnection connection;
    private volatile Map<Integer, EpgProgram> epgMap;
    private volatile Map<Date, HashMap<String, ArrayList<Integer>>> epgDateChannelMap;
    private volatile boolean loaded;
    
    EpgManager(TvHBrowser tvhBrowser, TVHeadendConnection connection, ChannelManager channelManager) {
        this.tvhBrowser = tvhBrowser;
        this.channelManager = channelManager;
        this.connection = connection;

        this.epgDateChannelMap = new HashMap<>();
        this.epgMap = new HashMap<>();
    }


    public boolean isReady() {
        return loaded;
    }

    public void loadEpgList() {
        Map<Integer, EpgProgram> newEpgMap = new HashMap<>();
        Map<Date, HashMap<String, ArrayList<Integer>>> newDateChannelMap = new HashMap<>();
        try {
            List<EpgProgram> epgList = this.connection.getEpgList();
            if (epgList == null) {
                tvhBrowser.writeLog("EPG could not be loaded, keeping previous data");
                return;
            }
            for (EpgProgram epgProgram : epgList) {
                epgProgram.extractEpisodeInfo();
                newDateChannelMap
                        .computeIfAbsent(epgProgram.getDate(), d -> new HashMap<>())
                        .computeIfAbsent(epgProgram.getChannelUuid(), c -> new ArrayList<>())
                        .add(epgProgram.getEventId());
                newEpgMap.put(epgProgram.getEventId(), epgProgram);
            }
            this.epgMap = newEpgMap;
            this.epgDateChannelMap = newDateChannelMap;
            tvhBrowser.writeLog("EPG loaded: " + newEpgMap.size() + " events");
        } finally {
            this.loaded = true;
        }
    }

    public EpgProgram getEpgProgram(int eventId) {
        return epgMap.get(eventId);
    }

    private List<EpgProgram> eventsOf(Program program, boolean warn) {
        Date programDate = program.getDate();
        TVHeadendChannel channel = channelManager.getMappedTvhChannel(program.getChannel().getUniqueId());
        if (channel == null) {
            tvhBrowser.writeLog("Channel not found for program " + program.getTitle());
            return null;
        }

        String channelKey = channel.getKey();
        HashMap<String, ArrayList<Integer>> channelMap = epgDateChannelMap.get(programDate);
        if (channelMap == null) {
            if (warn) {
                tvhBrowser.showWarning("No EPG data for date " + programDate.getShortDayLongMonthString());
            }
            return null;
        }

        ArrayList<Integer> eventIDList = channelMap.get(channelKey);
        if (eventIDList == null) {
            if (warn) {
                tvhBrowser.showWarning("No EPG data for channel " + channelKey);
            }
            return null;
        }

        List<EpgProgram> events = new ArrayList<>();
        for (Integer eventID : eventIDList) {
            EpgProgram epgProgram = epgMap.get(eventID);
            if (epgProgram != null) {
                events.add(epgProgram);
            }
        }
        return events;
    }

    public EpgProgram getEpgProgram(Program program) {
        if (program == null) {
            tvhBrowser.showError("Program is null");
            return null;
        }

        List<EpgProgram> events = eventsOf(program, true);
        if (events == null) {
            return null;
        }

        for (EpgProgram epgProgram : events) {
            if (epgProgram.compare(program)) {
                return epgProgram;
            }
        }

        return null;
    }

    public EpgProgram getSimilarEpgProgram(Program program) {
        List<EpgProgram> events = eventsOf(program, false);
        if (events == null) {
            return null;
        }

        EpgProgram best = null;
        long bestDistance = Long.MAX_VALUE;
        for (EpgProgram candidate : events) {
            if (candidate.compare(program, TimerEpgInterface.FALLBACK_TOLERANCE_SECONDS)) {
                long distance = candidate.timeDistanceSeconds(program);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    public List<EpgProgram> getSurroundingEpgPrograms(Program program) {
        List<EpgProgram> events = eventsOf(program, false);
        List<EpgProgram> result = new ArrayList<>();
        if (events == null) {
            return result;
        }

        int minute = program.getStartTime();
        LocalDateTime start = LocalDate.of(program.getDate().getYear(), program.getDate().getMonth(),
                program.getDate().getDayOfMonth()).atTime(minute / 60, minute % 60);

        EpgProgram before = null;
        EpgProgram after = null;
        for (EpgProgram e : events) {
            LocalDateTime t = e.getStartDateTime();
            if (!t.isAfter(start)) {
                if (before == null || t.isAfter(before.getStartDateTime())) {
                    before = e;
                }
            } else if (after == null || t.isBefore(after.getStartDateTime())) {
                after = e;
            }
        }

        if (before != null) {
            result.add(before);
        }
        if (after != null) {
            result.add(after);
        }
        return result;
    }

    public void updateEpg() {
        this.loadEpgList();
    }


}
