package tvhbrowser;

import java.util.*;

import devplugin.Date;
import devplugin.Program;
import tvhbrowser.*;

public class EpgManager {

    final private TvHBrowser tvhBrowser;
    final private ChannelManager channelManager;
    final private TVHeadendConnection connection;
    private Map<Integer, EpgProgram> epgMap;
    private Map<Date, HashMap<String, ArrayList<Integer>>> epgDateChannelMap;
    
    EpgManager(TvHBrowser tvhBrowser, TVHeadendConnection connection, ChannelManager channelManager) {
        this.tvhBrowser = tvhBrowser;
        this.channelManager = channelManager;
        this.connection = connection;

        this.epgDateChannelMap = new HashMap<>();
        this.epgMap = new HashMap<>();
    }


    public void loadEpgList() {
        epgMap.clear();
        epgDateChannelMap.clear();
        List<EpgProgram> epgList = this.connection.getEpgList();
        for (EpgProgram epgProgram : epgList) {
            epgProgram.extractEpisodeInfo();

            

            Date programDate = epgProgram.getDate();
            String channelUUID = epgProgram.getChannelUuid();
            int eventID = epgProgram.getEventId();

            // Check if the date exists in epgDateChannelMap
            if (!epgDateChannelMap.containsKey(programDate)) {
                epgDateChannelMap.put(programDate, new HashMap<>());
            }

            // Get the channel map for the date
            HashMap<String, ArrayList<Integer>> channelMap = epgDateChannelMap.get(programDate);

            // Check if the channel UUID exists in the channel map
            if (!channelMap.containsKey(channelUUID)) {
                channelMap.put(channelUUID, new ArrayList<>());
            }

            // Get the event ID list for the channel UUID
            ArrayList<Integer> eventIDList = channelMap.get(channelUUID);

            // Add the event ID to the list
            eventIDList.add(eventID);
            epgMap.put(eventID, epgProgram);
            tvhBrowser.writeLog("Added EPG program " + epgProgram.getTitle());
        }
    }

    public EpgProgram getEpgProgram(int eventId) {
        return epgMap.get(eventId);
    }

    public EpgProgram getEpgProgram(Program program) {

        if (program == null) {
            tvhBrowser.showError("Program is null");
            return null;
        }
        Date programDate = program.getDate();
        TVHeadendChannel channel = channelManager.getMappedTvhChannel(program.getChannel().getUniqueId());
        if(channel == null) {
            tvhBrowser.writeLog("Channel not found for program " + program.getTitle());
            return null;
        }

        String channelKey = channel.getKey();
        
        if (!epgDateChannelMap.containsKey(programDate)) {
            tvhBrowser.showWarning("No EPG data for date " + programDate.getShortDayLongMonthString());
            return null;
        }

        HashMap<String, ArrayList<Integer>> channelMap = epgDateChannelMap.get(programDate);

        if (!channelMap.containsKey(channelKey)) {
            tvhBrowser.showWarning("No EPG data for channel " + channel.getKey());
            return null;
        }

        tvhBrowser.writeLog("Channel key: " + channelKey);

        ArrayList<Integer> eventIDList = channelMap.get(channelKey);

        for (Integer eventID : eventIDList) {
            EpgProgram epgProgram = epgMap.get(eventID);
            if (epgProgram == null) {
                tvhBrowser.showError("EPG program not found for event ID " + eventID);
                continue;
            }

            if (epgProgram.compare(program)) {
                return epgProgram;
            }
        }

        return null;
    }

    public void updateEpg() {
        this.loadEpgList();
    }


}
