package tvhbrowser;

import java.util.Timer;
import java.util.TimerTask;

public class TVHTimerTask {
    private TvHBrowser tvHBrowser;
    private Timer timer;

    public TVHTimerTask(TvHBrowser tvHBrowser) {
        this.tvHBrowser = tvHBrowser;
    }

    public void startThread() {
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    tvHBrowser.updateTimer();
                }
            }, 0, 30000); // 60000 milliseconds = 1 minute
        }
    }

    public void stopThread() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
