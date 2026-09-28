package tvhbrowser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tvhbrowser.TestSupport.epoch;

import org.junit.Test;

import devplugin.Channel;
import devplugin.Date;
import devplugin.ProgramFieldType;

public class EpgChoiceProgramTest {

    private Channel channel() {
        Channel c = mock(Channel.class);
        when(c.getTimeZone()).thenReturn(java.util.TimeZone.getDefault());
        return c;
    }

    private EpgProgram epg() {
        EpgProgram e = new EpgProgram();
        e.setEventId(1);
        e.setTitle("Stuttgart 21");
        e.setSubtitle("Die Jahrhundertbaustelle");
        e.setDescription("Das teuerste Grossprojekt Deutschlands.");
        e.setStart(epoch(2026, 9, 28, 23, 35));
        e.setStop(epoch(2026, 9, 29, 0, 35));
        return e;
    }

    @Test
    public void adaptsEpgProgramForProgramList() {
        EpgProgram e = epg();
        EpgChoiceProgram p = new EpgChoiceProgram(channel(), e);

        assertSame(e, p.getEpgProgram());
        assertEquals("Stuttgart 21", p.getTitle());
        assertEquals(new Date(2026, 9, 28), p.getDate());
        assertEquals(23 * 60 + 35, p.getStartTime());
        assertEquals(60, p.getLength());
        assertEquals("Das teuerste Grossprojekt Deutschlands.", p.getDescription());
        assertEquals("Die Jahrhundertbaustelle", p.getTextField(ProgramFieldType.EPISODE_TYPE));
    }

    @Test
    public void handlesMissingOptionalFields() {
        EpgProgram e = new EpgProgram();
        e.setStart(epoch(2026, 9, 28, 10, 0));
        EpgChoiceProgram p = new EpgChoiceProgram(channel(), e);

        assertEquals("", p.getTitle());
        assertEquals(-1, p.getLength());
        assertNull(p.getTextField(ProgramFieldType.EPISODE_TYPE));
    }
}
