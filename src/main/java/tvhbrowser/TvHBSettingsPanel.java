
package tvhbrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import tvhbrowser.*;
import util.ui.ImageUtilities;
import util.i18n.Localizer;

/**
 * The root container for the settings tabs
 *
 * @author <a href="hampelratte@users.sf.net">hampelratte@users.sf.net </a>
 */
public class TvHBSettingsPanel implements devplugin.SettingsTab {

    private JTabbedPane tabbedPane;

    private ChannelPanel channelPanel;

    private GeneralPanel generalPanel;

    private TimerPanel timerPanel;

    private TimerManager timerManager;

    private ChannelManager channelManager;

    private TvHBrowser tvhBrowser;


    public TvHBSettingsPanel(TvHBrowser tvhBrowser,TimerManager timerManager, ChannelManager channelManager) {
        this.timerManager = timerManager;
        this.tvhBrowser = tvhBrowser;
        this.channelManager = channelManager;
    }

    @Override
    public JPanel createSettingsPanel() {
        
        if (tabbedPane == null) {
            tabbedPane = new JTabbedPane();
            tabbedPane.setPreferredSize(new Dimension(380, 380));

            generalPanel = new GeneralPanel(tvhBrowser);
            tabbedPane.addTab("General", generalPanel.getPanel());

            channelPanel = new ChannelPanel(tvhBrowser, channelManager);
            tabbedPane.addTab("Channels", channelPanel.getPanel());

            timerPanel = new TimerPanel(timerManager);
            tabbedPane.addTab("Timers", timerPanel.getPanel());
        }

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(tabbedPane, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void saveSettings() {
        // channelPanel.saveSettings();
        generalPanel.saveSettings();
        // playerPanel.saveSettings();
        // timerPanel.saveSettings();
        // previewPanel.saveSettings();
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(ImageUtilities.createImageFromJar("tvheadend.png", TvHBSettingsPanel.class));
    }

    @Override
    public String getTitle() {
        return "TVHeadend";
    }
}