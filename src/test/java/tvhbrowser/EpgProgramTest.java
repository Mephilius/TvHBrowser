package tvhbrowser;

import static org.junit.Assert.*;
import static tvhbrowser.TestSupport.*;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import devplugin.Date;

public class EpgProgramTest {

    @Test
    public void titlesNullWhenNoTitle() {
        assertNull(new EpgProgram().getTitles());
        assertNull(new EpgProgram().getSubtitles());
        assertNull(new EpgProgram().getExtractedTitles());
    }

    @Test
    public void titlesExposeGerAndEng() {
        EpgProgram e = new EpgProgram();
        e.setTitle("Tatort");
        e.setSubtitle("Folge");
        e.setExtractedTitle("ger", "T");
        assertEquals(map("ger", "Tatort", "eng", "Tatort"), e.getTitles());
        assertEquals(map("ger", "Folge", "eng", "Folge"), e.getSubtitles());
        assertEquals(map("ger", "T", "eng", "T"), e.getExtractedTitles());
        assertEquals("Tatort", e.getTitle("xyz"));
        assertEquals("Folge", e.getSubtitle("xyz"));
        assertEquals("T", e.getExtractedTitle("xyz"));
    }

    @Test
    public void extractEpisodeInfoWorks() {
        EpgProgram e = new EpgProgram();
        e.setTitle("Serie (2/8)");
        e.extractEpisodeInfo();
        assertEquals(2, e.getEpisodeNumber());
        assertEquals(8, e.getEpisodeNumberTotal());
        assertEquals("Serie", e.getExtractedTitle("ger"));
    }

    @Test
    public void extractEpisodeInfoWithoutTitleDoesNothing() {
        EpgProgram e = new EpgProgram();
        e.extractEpisodeInfo();
        assertEquals(0, e.getEpisodeNumber());
    }

    @Test
    public void dateAndStart() {
        EpgProgram e = new EpgProgram();
        e.setStart(epoch(2026, 9, 28, 12, 0));
        assertEquals(new Date(2026, 9, 28), e.getDate());
        assertEquals(epoch(2026, 9, 28, 12, 0), e.getStart());
        assertEquals(java.time.LocalDateTime.of(2026, 9, 28, 12, 0), e.getStartDateTime());
    }

    @Test
    public void accessorsRoundTrip() {
        EpgProgram e = new EpgProgram();
        e.setEventId(7);
        e.setChannelName("ARD");
        e.setChannelUuid("cu");
        e.setChannelNumber("1");
        e.setNextEventId(8);
        e.setHd(1);
        e.setWidescreen(1);
        e.setSubtitled(1);
        e.setAudiodesc(1);
        e.setGenre(List.of(1, 2));
        assertEquals(7, e.getEventId());
        assertEquals("ARD", e.getChannelName());
        assertEquals("cu", e.getChannelUuid());
        assertEquals("1", e.getChannelNumber());
        assertEquals(8, e.getNextEventId());
        assertEquals(1, e.getHd());
        assertEquals(1, e.getWidescreen());
        assertEquals(1, e.getSubtitled());
        assertEquals(1, e.getAudiodesc());
        assertEquals(List.of(1, 2), e.getGenre());
    }

    @Test
    public void deserializesTvheadendJson() {
        String json = "[{\"eventId\":5,\"channelUuid\":\"cu\",\"start\":1000,\"title\":\"Tatort\"}]";
        Type type = new TypeToken<List<EpgProgram>>() {
        }.getType();
        List<EpgProgram> l = new Gson().fromJson(json, type);
        assertEquals(5, l.get(0).getEventId());
        assertEquals("cu", l.get(0).getChannelUuid());
        assertEquals("Tatort", l.get(0).getTitle());
    }
}
