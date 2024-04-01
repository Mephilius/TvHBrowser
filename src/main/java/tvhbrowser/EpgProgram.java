package tvhbrowser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import devplugin.Date;

public class EpgProgram extends TimerEpgInterface {

    private int eventId;
    private String channelName;
    private String channelUuid;
    private String channelNumber;
    private String title;
    private String subtitle;
    private String extractedtitle;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    private int nextEventId;
    private int widescreen;
    private int subtitled;
    private int audiodesc;
    private int hd;
    private List<Integer> genre;

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelUuid() {
        return channelUuid;
    }

    public void setChannelUuid(String channelUuid) {
        this.channelUuid = channelUuid;
    }

    public String getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(String channelNumber) {
        this.channelNumber = channelNumber;
    }

    public long getStart() {
        return start;
    }

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(this.start), ZoneOffset.systemDefault());
    }

    private Calendar convertToCalendar(long start) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start * 1000);
        return cal;
    }

    public Date getDate() {
        return new Date(convertToCalendar(this.start));
    }

    public void setStart(long start) {
        this.start = start;
    }

    public int getNextEventId() {
        return nextEventId;
    }

    public void setNextEventId(int nextEventId) {
        this.nextEventId = nextEventId;
    }

    public int getWidescreen() {
        return widescreen;
    }

    public void setWidescreen(int widescreen) {
        this.widescreen = widescreen;
    }

    public int getSubtitled() {
        return subtitled;
    }

    public void setSubtitled(int subtitled) {
        this.subtitled = subtitled;
    }

    public int getAudiodesc() {
        return audiodesc;
    }

    public void setAudiodesc(int audiodesc) {
        this.audiodesc = audiodesc;
    }

    public int getHd() {
        return hd;
    }

    public void setHd(int hd) {
        this.hd = hd;
    }

    public List<Integer> getGenre() {
        return genre;
    }

    public void setGenre(List<Integer> genre) {
        this.genre = genre;
    }

    @Override
    public String getTitle(String Lang) {
        return this.title;
    }

    @Override
    public Map<String, String> getTitles() {
        if (this.title == null) {
            return null;

        };
        return Map.ofEntries(
                Map.entry("ger", this.title),
                Map.entry("eng", this.title));

    };

    @Override
    public String getSubtitle(String Lang) {
        return this.subtitle;
    }

    @Override
    public Map<String, String> getSubtitles() {
        if (this.subtitle == null) {
            return null;

        };
        return Map.ofEntries(
                Map.entry("ger", this.subtitle),
                Map.entry("eng", this.subtitle));

    };

    @Override
    public void setExtractedTitle(String Lang, String Title) {
        this.extractedtitle = Title;
    }

    @Override
    public String getExtractedTitle(String Lang) {
        return this.extractedtitle;
    }

    @Override
    public Map<String, String> getExtractedTitles() {
        if (this.extractedtitle == null) {
            return null;

        };
        return Map.ofEntries(
                Map.entry("ger", this.extractedtitle),
                Map.entry("eng", this.extractedtitle));

    };

}
