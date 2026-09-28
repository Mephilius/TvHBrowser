package tvhbrowser;

import static org.mockito.Mockito.*;

import org.junit.Test;

public class TVHTimerTaskTest {

    @Test
    public void startThreadTriggersUpdateImmediately() {
        TvHBrowser b = mock(TvHBrowser.class);
        TVHTimerTask task = new TVHTimerTask(b);
        try {
            task.startThread();
            verify(b, timeout(2000).atLeastOnce()).updateTimer();
        } finally {
            task.stopThread();
        }
    }

    @Test
    public void startThreadTwiceKeepsSingleSchedule() throws Exception {
        TvHBrowser b = mock(TvHBrowser.class);
        TVHTimerTask task = new TVHTimerTask(b);
        try {
            task.startThread();
            task.startThread();
            verify(b, timeout(2000).atLeastOnce()).updateTimer();
            Thread.sleep(200);
            verify(b, times(1)).updateTimer();
        } finally {
            task.stopThread();
        }
    }

    @Test
    public void stopThreadWithoutStartIsSafe() {
        new TVHTimerTask(mock(TvHBrowser.class)).stopThread();
    }

    @Test
    public void canRestartAfterStop() {
        TvHBrowser b = mock(TvHBrowser.class);
        TVHTimerTask task = new TVHTimerTask(b);
        try {
            task.startThread();
            task.stopThread();
            reset(b);
            task.startThread();
            verify(b, timeout(2000).atLeastOnce()).updateTimer();
        } finally {
            task.stopThread();
        }
    }
}
