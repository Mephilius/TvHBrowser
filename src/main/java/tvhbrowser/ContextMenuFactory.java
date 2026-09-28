package tvhbrowser;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import devplugin.ActionMenu;
import devplugin.Marker;
import devplugin.Program;
import util.ui.ImageUtilities;

public class ContextMenuFactory {

    TvHBrowser tvhBrowser;
    ChannelManager channelManager;
    TimerManager timerManager;

    ContextMenuFactory(TvHBrowser tvhBrowser, ChannelManager channelManager, TimerManager timerManager) {
        this.tvhBrowser = tvhBrowser;
        this.channelManager = channelManager;
        this.timerManager = timerManager;
    }

    public ActionMenu createActionMenu(Program program) {

        boolean isOnAir = program.isOnAir();
        Marker[] markers = program.getMarkerArr();
        boolean marked = false;
        for (int i = 0; i < markers.length; i++) {
            if (markers[i].getId().equals(this.tvhBrowser.getId())) {
                marked = true;
                break;
            }
        }

        ActionMenu[] actions;

        // Recording action (Recording/Stop recording)
        AbstractAction record = new AbstractAction() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (e.getActionCommand().equals("Stop recording") || e.getActionCommand().equals("Delete Timer")) {
                    timerManager.deleteTimer(program);
                } else {
                    timerManager.createTimer(program);
                }
            }
        };

        // Recording action (Enable/Disable recording)
        AbstractAction enable = new AbstractAction() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (e.getActionCommand().equals("Disable Timer")) {
                    timerManager.disableTimer(program);
                } else {
                    timerManager.enableTimer(program);
                }
            }
        };

        if (marked) {

            if (isOnAir) {
                record.putValue(Action.NAME, "Stop recording");
                record.putValue(Action.SMALL_ICON,
                        new ImageIcon(ImageUtilities.createImageFromJar("stop.png", TvHBrowser.class)));
            } else {
                record.putValue(Action.NAME, "Delete Timer");
                record.putValue(Action.SMALL_ICON,
                        new ImageIcon(ImageUtilities.createImageFromJar("delete_timer.png", TvHBrowser.class)));
                boolean timerEnabled = timerManager.isTimerEnabled(program);
                if (timerEnabled) {
                    enable.putValue(Action.NAME, "Disable Timer");
                    enable.putValue(Action.SMALL_ICON,
                            new ImageIcon(ImageUtilities.createImageFromJar("timer_disabled.png", TvHBrowser.class)));
                } else {
                    enable.putValue(Action.NAME, "Enable Timer");
                    enable.putValue(Action.SMALL_ICON,
                            new ImageIcon(ImageUtilities.createImageFromJar("timer_enabled.png", TvHBrowser.class)));
                }
            }
        } else {
            if (isOnAir) {
                record.putValue(Action.NAME, "Recording running Program");
                record.putValue(Action.SMALL_ICON,
                        new ImageIcon(ImageUtilities.createImageFromJar("recording.png", TvHBrowser.class)));
            } else {
                record.putValue(Action.NAME, "Recording");
                record.putValue(Action.SMALL_ICON,
                        new ImageIcon(ImageUtilities.createImageFromJar("recording.png", TvHBrowser.class)));
            }
        }

        if (marked && !isOnAir) {
            actions = new ActionMenu[] {
                new ActionMenu(record),
                new ActionMenu(enable)
            };
        } else {
            actions = new ActionMenu[] {
                new ActionMenu(record)
            };
        }

        return new ActionMenu("Tvheadend",
                new ImageIcon(ImageUtilities.createImageFromJar("tvheadend.png", TvHBrowser.class)), actions);

    }

}
