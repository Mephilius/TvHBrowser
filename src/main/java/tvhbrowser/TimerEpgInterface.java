package tvhbrowser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
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

    /**
     * Lower-cases and strips punctuation so that differently formatted titles
     * ("Foo: Bar", "Foo - Bar", "Foo (Bar)") compare equal, while still leaving a
     * word boundary (a space) where the punctuation was.
     */
    static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String result = text.toLowerCase(Locale.ROOT);
        result = result.replaceAll("[„“”‚‘'`´\"]", "");
        result = result.replaceAll("[:;,.!?/\\-–—()\\[\\]]", " ");
        result = result.replaceAll("\\s+", " ").trim();
        return result;
    }

    public ArrayList<String> getProgramTextToCompare(Program program) {
        ArrayList<String> values = new ArrayList<>();
        if (program.getTitle() != null) {
            values.add(normalize(program.getTitle()));
        }
        if (program.getTextField(devplugin.ProgramFieldType.EPISODE_TYPE) != null) {
            values.add(normalize(program.getTextField(devplugin.ProgramFieldType.EPISODE_TYPE)));
        }
        if (program.getTextField(devplugin.ProgramFieldType.SERIES_TYPE) != null) {
            values.add(normalize(program.getTextField(devplugin.ProgramFieldType.SERIES_TYPE)));
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
                outputList.add(normalize(title));
            }

            // Add subtitle to output list
            if (subtitle != null) {
                outputList.add(normalize(subtitle));
            }

            // Add combination of title and subtitle to output list
            if (title != null && subtitle != null) {
                outputList.add(normalize(title + " " + subtitle));
            }

            // Add combination of extracted title and subtitle to output list
            if (extractedTitle != null && subtitle != null) {
                outputList.add(normalize(extractedTitle + " " + subtitle));
            }
        }

        return outputList;
    }

    public static final long STRICT_TOLERANCE_SECONDS = 300;
    public static final long FALLBACK_TOLERANCE_SECONDS = 3600;

    /** Sentinel returned by {@link #matchScore} when the program does not match at all. */
    public static final long NO_MATCH = Long.MIN_VALUE;

    private static final int MIN_PREFIX_LENGTH = 3;

    public long timeDistanceSeconds(Program program) {
        long secondOfDayStartTimer = getStartDateTime().toLocalTime().toSecondOfDay();
        long secondOfDayProgram = program.getStartTime() * 60L;
        return Math.abs(secondOfDayProgram - secondOfDayStartTimer);
    }

    public Boolean compare(Program program) {
        return compare(program, STRICT_TOLERANCE_SECONDS);
    }

    public Boolean compare(Program program, long toleranceSeconds) {
        return matchScore(program, toleranceSeconds) != NO_MATCH;
    }

    /**
     * Scores how well the given program matches this timer/EPG entry, or returns
     * {@link #NO_MATCH} if it doesn't match at all. Higher scores are better
     * matches; callers that consider several candidate programs should pick the
     * one with the highest score instead of the first one that matches.
     */
    public long matchScore(Program program, long toleranceSeconds) {
        long timeDistance = timeDistanceSeconds(program);
        if (timeDistance > toleranceSeconds) {
            return NO_MATCH;
        }

        int programEpisode = program.getIntField(devplugin.ProgramFieldType.EPISODE_NUMBER_TYPE);
        boolean bothHaveEpisodeNumber = programEpisode > 0 && this.episodeNumber > 0;
        if (bothHaveEpisodeNumber && programEpisode != this.episodeNumber) {
            return NO_MATCH;
        }

        int textScore = -1;
        for (String timerText : getTimerTextToCompare()) {
            for (String programText : getProgramTextToCompare(program)) {
                textScore = Math.max(textScore, textMatchScore(timerText, programText));
            }
        }
        if (textScore < 0) {
            return NO_MATCH;
        }

        long score = (long) textScore * 100_000L - timeDistance;
        if (bothHaveEpisodeNumber) {
            score += 10_000_000L;
        }
        return score;
    }

    /**
     * Compares two normalized texts. Returns 100 for an exact match, a positive
     * score for a prefix match at a word boundary (e.g. "tatort" is a valid
     * prefix of "tatort der fall", but not of "tatortreiniger"), or -1 if the
     * texts don't match.
     */
    private static int textMatchScore(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return -1;
        }
        if (a.equals(b)) {
            return 100;
        }

        String shorter = a.length() <= b.length() ? a : b;
        String longer = a.length() <= b.length() ? b : a;
        if (shorter.length() < MIN_PREFIX_LENGTH || !longer.startsWith(shorter)) {
            return -1;
        }
        if (Character.isLetterOrDigit(longer.charAt(shorter.length()))) {
            // the shorter text is only a prefix of a longer word, e.g. "sport" in "sportschau"
            return -1;
        }

        return Math.max(10, 90 - (longer.length() - shorter.length()));
    }

}
