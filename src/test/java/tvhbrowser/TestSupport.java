package tvhbrowser;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import devplugin.Program;

final class TestSupport {
    private TestSupport() {
    }

    static long epoch(int y, int mo, int d, int h, int mi) {
        return LocalDateTime.of(y, mo, d, h, mi).atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    static Timer timer(String uuid, String channel, long start, String lang, String title) {
        Timer t = new Timer();
        t.setUuid(uuid);
        t.setChannel(channel);
        t.setStart(start);
        t.setTitle(map(lang, title));
        return t;
    }

    static Program program(String title, int startMinute) {
        Program p = mock(Program.class);
        when(p.getTitle()).thenReturn(title);
        when(p.getStartTime()).thenReturn(startMinute);
        return p;
    }

    static TvHBrowser browserWithSettings(String host, int port) {
        TvHBrowser b = mock(TvHBrowser.class);
        when(b.getSetting(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(b.getSetting(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        when(b.getSetting(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        when(b.getSetting(eq("fqdn"), anyString())).thenReturn(host);
        when(b.getSetting(eq("port"), anyInt())).thenReturn(port);
        when(b.getSetting(eq("username"), anyString())).thenReturn("user");
        when(b.getSetting(eq("password"), anyString())).thenReturn("pw");
        return b;
    }
}
