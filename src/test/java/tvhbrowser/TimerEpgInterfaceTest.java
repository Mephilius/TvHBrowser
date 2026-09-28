package tvhbrowser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static tvhbrowser.TestSupport.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import devplugin.Program;
import devplugin.ProgramFieldType;

public class TimerEpgInterfaceTest {

    private static final long START = epoch(2026, 9, 28, 20, 15);
    private static final int START_MIN = 20 * 60 + 15;

    @Test
    public void startDateTimeConvertsEpochSeconds() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertEquals(LocalDateTime.of(2026, 9, 28, 20, 15), t.getStartDateTime());
    }

    @Test
    public void extractEpisodeInfoParsesNumberAndTotal() {
        Timer t = timer("u", "c", START, "ger", "Serie (3/12)");
        t.extractEpisodeInfo();
        assertEquals(3, t.getEpisodeNumber());
        assertEquals(12, t.getEpisodeNumberTotal());
        assertEquals("Serie", t.getExtractedTitle("ger"));
    }

    @Test
    public void extractEpisodeInfoIgnoresTitlesWithoutPattern() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        t.extractEpisodeInfo();
        assertEquals(0, t.getEpisodeNumber());
        assertNull(t.getExtractedTitles());
    }

    @Test
    public void extractEpisodeInfoHandlesMissingTitles() {
        Timer t = new Timer();
        t.extractEpisodeInfo();
        assertNull(t.getExtractedTitles());
    }

    @Test
    public void timerTextContainsTitleSubtitleAndCombinations() {
        Timer t = timer("u", "c", START, "ger", "Serie (3/12)");
        t.setSubtitle(map("ger", "Folge"));
        t.extractEpisodeInfo();
        List<String> texts = t.getTimerTextToCompare();
        assertEquals(new HashSet<>(List.of("serie (3/12)", "folge", "serie (3/12) folge", "serie folge")),
                new HashSet<>(texts));
    }

    @Test
    public void timerTextWithOnlyTitle() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertEquals(List.of("tatort"), t.getTimerTextToCompare());
    }

    @Test
    public void timerTextEmptyWithoutAnyText() {
        assertTrue(new Timer().getTimerTextToCompare().isEmpty());
    }

    @Test
    public void programTextIsLowerCasedAndSkipsNulls() {
        Timer t = new Timer();
        Program p = program("Tatort", START_MIN);
        when(p.getTextField(ProgramFieldType.EPISODE_TYPE)).thenReturn("Folge A");
        assertEquals(List.of("tatort", "folge a"), t.getProgramTextToCompare(p));
    }

    @Test
    public void programTextIncludesSeries() {
        Timer t = new Timer();
        Program p = program(null, START_MIN);
        when(p.getTextField(ProgramFieldType.SERIES_TYPE)).thenReturn("Reihe");
        assertEquals(List.of("reihe"), t.getProgramTextToCompare(p));
    }

    @Test
    public void compareMatchesSameTitleAndTime() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertTrue(t.compare(program("Tatort", START_MIN)));
    }

    @Test
    public void compareToleratesFiveMinutesDifference() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertTrue(t.compare(program("Tatort", START_MIN + 5)));
        assertTrue(t.compare(program("Tatort", START_MIN - 5)));
    }

    @Test
    public void compareRejectsMoreThanFiveMinutesDifference() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertFalse(t.compare(program("Tatort", START_MIN + 6)));
    }

    @Test
    public void compareRejectsDifferentTitle() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertFalse(t.compare(program("Heute Journal", START_MIN)));
    }

    @Test
    public void compareMatchesWhenProgramTitleIsPrefixOfTimerText() {
        Timer t = timer("u", "c", START, "ger", "Tatort: Der Fall");
        assertTrue(t.compare(program("Tatort", START_MIN)));
    }

    @Test
    public void compareMatchesWhenTimerTextIsPrefixOfProgramTitle() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertTrue(t.compare(program("Tatort: Der Fall", START_MIN)));
    }

    @Test
    public void compareIsCaseInsensitive() {
        Timer t = timer("u", "c", START, "ger", "TATORT");
        assertTrue(t.compare(program("tatort", START_MIN)));
    }

    @Test
    public void compareChecksEpisodeNumberWhenBothKnown() {
        Timer t = timer("u", "c", START, "ger", "Serie (3/12)");
        t.extractEpisodeInfo();
        Program same = program("Serie", START_MIN);
        when(same.getIntField(ProgramFieldType.EPISODE_NUMBER_TYPE)).thenReturn(3);
        assertTrue(t.compare(same));

        Program other = program("Serie", START_MIN);
        when(other.getIntField(ProgramFieldType.EPISODE_NUMBER_TYPE)).thenReturn(4);
        assertFalse(t.compare(other));
    }

    @Test
    public void compareIgnoresEpisodeNumberWhenProgramHasNone() {
        Timer t = timer("u", "c", START, "ger", "Serie (3/12)");
        t.extractEpisodeInfo();
        assertTrue(t.compare(program("Serie", START_MIN)));
    }

    @Test
    public void compareAppliesToEpgProgramToo() {
        EpgProgram e = new EpgProgram();
        e.setTitle("Tatort");
        e.setStart(START);
        assertTrue(e.compare(program("Tatort", START_MIN)));
        assertFalse(e.compare(program("Anderes", START_MIN)));
    }

    @Test
    public void stopAndEpisodeAccessors() {
        Timer t = new Timer();
        t.setStop(42L);
        t.setEpisodeNumber(2);
        t.setEpisodeNumberTotal(9);
        assertEquals(42L, t.getStop());
        assertEquals(2, t.getEpisodeNumber());
        assertEquals(9, t.getEpisodeNumberTotal());
    }

    @Test
    public void compareWithWiderToleranceAcceptsFifteenMinutes() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        Program p = program("Tatort", START_MIN + 15);
        assertFalse(t.compare(p));
        assertTrue(t.compare(p, TimerEpgInterface.FALLBACK_TOLERANCE_SECONDS));
        assertEquals(15 * 60, t.timeDistanceSeconds(p));
    }

    @Test
    public void compareWithWiderToleranceStillNeedsTextMatch() {
        Timer t = timer("u", "c", START, "ger", "Tatort");
        assertFalse(t.compare(program("Anderes", START_MIN + 15), TimerEpgInterface.FALLBACK_TOLERANCE_SECONDS));
    }
}
