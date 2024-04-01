package tvhbrowser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import devplugin.Program;

public abstract class TimerEpgInterface {
    // Class implementation

    protected long start;
    private long stop;

    protected int episodeNumber;
    protected int episodeNumberTotal;

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public int getEpisodeNumberTotal() {
        return episodeNumberTotal;
    }

    public abstract String getTitle(String Lang);

    public abstract Map<String, String> getTitles();

    public abstract String getSubtitle(String Lang);

    public abstract Map<String, String> getSubtitles();

    public abstract String getExtractedTitle(String Lang);

    public abstract Map<String, String> getExtractedTitles();

    public abstract void setExtractedTitle(String Lang, String Title);

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(this.start), ZoneOffset.systemDefault());
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public void setEpisodeNumberTotal(int episodeNumberTotal) {
        this.episodeNumberTotal = episodeNumberTotal;
    }

    // Getter and Setter methods for stop
    public long getStop() {
        return stop;
    }

    public void setStop(long stop) {
        this.stop = stop;
    }

    public void extractEpisodeInfo() {
        Pattern pattern = Pattern.compile("(.*) \\((\\d+)\\/(\\d+)\\)");

        if (getTitles() == null) {
            return;
        }
        
        for (Map.Entry<String, String> title : getTitles().entrySet()) {
            String key = title.getKey();
            String value = title.getValue();

            Matcher matcher = pattern.matcher(value);

            if (matcher.find()) {
                String extractedTitle = matcher.group(1);
                String episodeNumber = matcher.group(2);
                String totalEpisodes = matcher.group(3);

                int episodeNumberInt = Integer.parseInt(episodeNumber);
                int totalEpisodesInt = Integer.parseInt(totalEpisodes);

                // Store the extracted values
                setEpisodeNumber(episodeNumberInt);
                setEpisodeNumberTotal(totalEpisodesInt);

                // Set the extracted title
                this.setExtractedTitle(key, extractedTitle);
            }
        }

    }

    public ArrayList<String> getProgramTextToCompare(Program program) {
        ArrayList<String> values = new ArrayList<>();
        if (program.getTitle() != null) {
            values.add(program.getTitle().toLowerCase());
        }
        if (program.getTextField(devplugin.ProgramFieldType.EPISODE_TYPE) != null) {
            values.add(program.getTextField(devplugin.ProgramFieldType.EPISODE_TYPE).toLowerCase());
        }
        if (program.getTextField(devplugin.ProgramFieldType.SERIES_TYPE) != null) {
            values.add(program.getTextField(devplugin.ProgramFieldType.SERIES_TYPE).toLowerCase());
        }
        return values;
    }

    public ArrayList<String> getTimerTextToCompare() {
        ArrayList<String> lang = new ArrayList<>();

        // Get all keys of the title map if not null
        if (getTitles() != null) {
            lang.addAll(getTitles().keySet());
        }

        // Get all keys of the subtitle map if not null
        if (getSubtitles() != null) {
            lang.addAll(getSubtitles().keySet());
        }

        // Get all keys of the extracted title map if not null
        if (getExtractedTitles() != null) {
            lang.addAll(getExtractedTitles().keySet());
        }

        // Remove duplicates from the list
        lang = new ArrayList<>(new HashSet<>(lang));

        ArrayList<String> outputList = new ArrayList<>();

        for (String text : lang) {
            String title = getTitles() != null ? getTitles().get(text) : null;
            String subtitle = getSubtitles() != null ? getSubtitles().get(text) : null;
            String extractedTitle = getExtractedTitles() != null ? getExtractedTitles().get(text) : null;

            // Add title to output list
            if (title != null) {
                outputList.add(title.toLowerCase());
            }

            // Add subtitle to output list
            if (subtitle != null) {
                outputList.add(subtitle.toLowerCase());
            }

            // Add combination of title and subtitle to output list
            if (title != null && subtitle != null) {
                outputList.add((title + " " + subtitle).toLowerCase());
            }

            // Add combination of extracted title and subtitle to output list
            if (extractedTitle != null && subtitle != null) {
                outputList.add((extractedTitle + " " + subtitle).toLowerCase());
            }
        }

        return outputList;
    }

    public Boolean compare(Program program) {

        long secondOfDayStartTimer = getStartDateTime().toLocalTime().toSecondOfDay();
        // long secondOfDayStartRealTimer =
        // getStartRealDateTime().toLocalTime().toSecondOfDay();
        long secondOfDayProgram = program.getStartTime() * 60;

        if (Math.abs(secondOfDayProgram - secondOfDayStartTimer) <= 300) {

            ArrayList<String> timerTextToCompare = getTimerTextToCompare();
            ArrayList<String> programTextToCompare = getProgramTextToCompare(program);

            for (String timerText : timerTextToCompare) {
                for (String programText : programTextToCompare) {
                    if (timerText.startsWith(programText) || programText.startsWith(timerText)) {

                        if( program.getIntField(devplugin.ProgramFieldType.EPISODE_NUMBER_TYPE) > 0 && this.getEpisodeNumber() > 0)
                        {
                            if( program.getIntField(devplugin.ProgramFieldType.EPISODE_NUMBER_TYPE) == this.getEpisodeNumber() )
                            {
                                return true;
                            }
                        }
                        else
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

}
