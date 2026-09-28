package tvhbrowser;

import static org.junit.Assert.*;
import static tvhbrowser.TestSupport.*;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import devplugin.Date;

public class TimerTest {

    @Test
    public void getDateUsesStartTimestamp() {
        Timer t = new Timer();
        t.setStart(epoch(2026, 9, 28, 23, 30));
        assertEquals(new Date(2026, 9, 28), t.getDate());
    }

    @Test
    public void titleAccessors() {
        Timer t = timer("u", "c", 0, "ger", "Tatort");
        assertEquals("Tatort", t.getTitle("ger"));
        assertNull(t.getTitle("eng"));
        assertSame(t.getTitle(), t.getTitles());
    }

    @Test
    public void subtitleAccessors() {
        Timer t = new Timer();
        t.setSubtitle(map("ger", "Folge"));
        assertEquals("Folge", t.getSubtitle("ger"));
        assertEquals(t.getSubtitle(), t.getSubtitles());
    }

    @Test
    public void setExtractedTitleCreatesMap() {
        Timer t = new Timer();
        assertNull(t.getExtractedTitles());
        t.setExtractedTitle("ger", "X");
        assertEquals("X", t.getExtractedTitle("ger"));
    }

    @Test
    public void simpleAccessorsRoundTrip() {
        Timer t = new Timer();
        t.setUuid("abc");
        t.setEnabled(true);
        t.setChannel("ch");
        t.setChannelname("Das Erste");
        t.setStart_real(10L);
        t.setDuration(3600);
        t.setStatus("scheduled");
        assertEquals("abc", t.getUuid());
        assertTrue(t.isEnabled());
        assertEquals("ch", t.getChannel());
        assertEquals("Das Erste", t.getChannelname());
        assertEquals(10L, t.getStart_real());
        assertEquals(3600, t.getDuration());
        assertEquals("scheduled", t.getStatus());
    }

    @Test
    public void startRealDateTimeConverts() {
        Timer t = new Timer();
        t.setStart_real(epoch(2026, 1, 2, 3, 4));
        assertEquals(java.time.LocalDateTime.of(2026, 1, 2, 3, 4), t.getStartRealDateTime());
    }

    @Test
    public void deserializesTvheadendJson() {
        String json = "[{\"uuid\":\"u1\",\"enabled\":true,\"start\":1000,\"channel\":\"c1\","
                + "\"channelname\":\"ARD\",\"title\":{\"ger\":\"Tatort\"},\"subtitle\":{\"ger\":\"Folge\"},"
                + "\"unknownField\":1}]";
        Type type = new TypeToken<List<Timer>>() {
        }.getType();
        List<Timer> timers = new Gson().fromJson(json, type);
        assertEquals(1, timers.size());
        Timer t = timers.get(0);
        assertEquals("u1", t.getUuid());
        assertTrue(t.isEnabled());
        assertEquals(1000L, t.getStart());
        assertEquals("Tatort", t.getTitle("ger"));
        assertEquals("Folge", t.getSubtitle("ger"));
    }
}
