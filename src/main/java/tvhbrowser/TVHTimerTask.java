package tvhbrowser;

import java.util.Timer;
import java.util.TimerTask;

public class TVHTimerTask {
    static final long POLL_INTERVAL_MS = 30_000;

    private final TvHBrowser tvHBrowser;
    private Timer timer;

    public TVHTimerTask(TvHBrowser tvHBrowser) {
        this.tvHBrowser = tvHBrowser;
    }

    public synchronized void startThread() {
        if (timer == null) {
            timer = new Timer("tvh-poll", true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    tvHBrowser.updateTimer();
                }
            }, 0, POLL_INTERVAL_MS);
        }
    }

    public synchronized void stopThread() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
