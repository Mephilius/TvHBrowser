package tvhbrowser;

import javax.faces.event.ActionEvent;
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

        ActionMenu[] actions = new ActionMenu[2];

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

        if (marked) {

            if (isOnAir) {
                record.putValue(Action.NAME, "Stop recording");
                record.putValue(Action.SMALL_ICON,
                        new ImageIcon(ImageUtilities.createImageFromJar("stop.png", TvHBrowser.class)));
            } else {
                record.putValue(Action.NAME, "Delete Timer");
                record.putValue(Action.SMALL_ICON,
                        new ImageIcon(ImageUtilities.createImageFromJar("delete_timer.png", TvHBrowser.class)));
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

        AbstractAction setting = new AbstractAction() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                timerManager.deleteTimer(program);
            }
        };
        setting.putValue(Action.NAME, "Settings");
        setting.putValue(Action.SMALL_ICON,
                new ImageIcon(ImageUtilities.createImageFromJar("tvheadend.png", TvHBrowser.class)));

        actions[0] = new ActionMenu(record);
        actions[1] = new ActionMenu(setting);

        ActionMenu menu = new ActionMenu("Tvheadend", actions);

        // menu.

        // menu.putValue(Action.NAME, "Settings");
        // menu.putValue(Action.SMALL_ICON, tvhBrowser.createImageIcon("actions",
        // "tvheadend.png", 16));

        return new ActionMenu("Tvheadend",
                new ImageIcon(ImageUtilities.createImageFromJar("tvheadend.png", TvHBrowser.class)), actions);

    }

    public ActionMenu createChannelActionMenu(final devplugin.Channel chan) {
        final TVHeadendChannel tvHeadendChannel = this.channelManager.getMappedTvhChannel(chan.getUniqueId());

        ActionMenu[] actions = new ActionMenu[2];
        AbstractAction record = new AbstractAction() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'actionPerformed'");
            }
        };

        AbstractAction setting = new AbstractAction() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'actionPerformed'");
            }
        };

        record.putValue(Action.NAME, "Aufnehmen");
        record.putValue(Action.SMALL_ICON,
                new ImageIcon(ImageUtilities.createImageFromJar("tvheadend.png", TvHBrowser.class)));
        actions[0] = new ActionMenu(record);
        actions[1] = new ActionMenu(setting);

        return new ActionMenu("Tvheadend", actions);

    }

}
