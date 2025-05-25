
/**
 * The `TvHBrowser` class is a plugin that exports program schedules from TV-Browser to Tvheadend.
 * It provides functionality for configuring settings, managing timers, and interacting with Tvheadend.
 * 
 * This plugin is developed by Markus Muerling (mephilius@muerling.de) and is licensed under AGPL-3.0.
 * For more information, visit the project website: https://muerling.de/TvHBrowser
 * 
 * @author Markus Muerling
 * @copyright 2024
 */

package tvhbrowser;

import devplugin.*;
import util.ui.EnhancedPanelBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The `TvHBrowser` class is a plugin that exports program schedules from
 * TV-Browser to Tvheadend.
 * It provides functionality for configuring settings, managing timers, and
 * interacting with Tvheadend.
 * 
 * This plugin is developed by Markus Muerling (mephilius@muerling.de) and is
 * licensed under AGPL-3.0.
 * For more information, visit the project website:
 * https://muerling.de/TvHBrowser
 */
public final class TvHBrowser extends Plugin {

    private final static Version VERSION = new Version(0, 1, true);
    private final static PluginInfo INFO = new PluginInfo(TvHBrowser.class,
            "TVHBrowser",
            "Exports program schedules from TV-Browser to Tvheadend",
            "Markus Muerling (mephilius@muerling.de)",
            "AGPL-3.0",
            "https://muerling.de/TvHBrowser");
    private final TvHBSettingsPanel settingsPanel;
    private final long mainThreadId = Thread.currentThread().getId();

    private final TimerManager timerManager;

    private final ChannelManager channelManager;

    private static Properties settings;

    private final TVHeadendConnection tvHeadendConnection;

    private final TVHTimerTask timerTask;

    private final ContextMenuFactory cmf;

    private final EpgManager epgManager;

    /** Translator */
    private static final util.i18n.Localizer mLocalizer = util.i18n.Localizer.getLocalizerFor(TvHBrowser.class);

    public TvHBrowser() {
        settings = new Properties();

        this.tvHeadendConnection = new TVHeadendConnection(this);

        this.channelManager = new ChannelManager(this, tvHeadendConnection);
        this.epgManager = new EpgManager(this, tvHeadendConnection, channelManager);
        this.timerManager = new TimerManager(this, tvHeadendConnection, channelManager, epgManager);
        this.settingsPanel = new TvHBSettingsPanel(this, timerManager, channelManager);

        this.cmf = new ContextMenuFactory(this, channelManager, timerManager);

        this.timerTask = new TVHTimerTask(this);
    }

    @Override
    public devplugin.SettingsTab getSettingsTab() {
        return new TvHBSettingsPanel(this, timerManager, channelManager);
    }

    public static Version getVersion() {
        return VERSION;
    }

    @Override
    public final PluginInfo getInfo() {
        return INFO;
    }

    @Override
    public ActionMenu getContextMenuActions(final Program program) {
        return cmf.createActionMenu(program);
    }

    // @Override
    // public ActionMenu getContextMenuActions(final devplugin.Channel channel) {
    // return cmf.createChannelActionMenu(channel);
    // }

    @Override
    public final String getPluginCategory() {
        return CATEGORY_REMOTE_CONTROL_SOFTWARE;
    }

    public static String getTranslation(String key, String altText) {
        return mLocalizer.msg(key, altText);
    }

    public static String getTranslation(String key, String altText, String arg1) {
        return mLocalizer.msg(key, altText, arg1);
    }

    public static String getTranslation(String key, String altText, String arg1, String arg2) {
        return mLocalizer.msg(key, altText, arg1, arg2);
    }

    public static String getTranslation(String key, String altText, String arg1, String arg2, String arg3) {
        return mLocalizer.msg(key, altText, arg1, arg2, arg3);
    }

    public Properties getSettings() {
        return settings;
    }

    public void updateSettings(Properties newSettings) {
        settings = newSettings;
    }

    public void setSetting(String key, String value) {
        settings.setProperty(key, value);
    }

    public String getSetting(String key, String defaultValue) {
        return settings.getProperty(key, defaultValue);
    }

    public void setSetting(String key, int value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public int getSetting(String key, int defaultValue) {
        return Integer.parseInt(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setSetting(String key, boolean value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public boolean getSetting(String key, boolean defaultValue) {
        return Boolean.parseBoolean(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setSetting(String key, long value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public long getSetting(String key, long defaultValue) {
        return Long.parseLong(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setSetting(String key, double value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public double getSetting(String key, double defaultValue) {
        return Double.parseDouble(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setSetting(String key, float value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public float getSetting(String key, float defaultValue) {
        return Float.parseFloat(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setSetting(String key, char value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public char getSetting(String key, char defaultValue) {
        String value = settings.getProperty(key);
        return (value != null && !value.isEmpty()) ? value.charAt(0) : defaultValue;
    }

    public void setSetting(String key, short value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public short getSetting(String key, short defaultValue) {
        return Short.parseShort(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setSetting(String key, byte value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public byte getSetting(String key, byte defaultValue) {
        return Byte.parseByte(settings.getProperty(key, String.valueOf(defaultValue)));
    }

    @Override
    public Properties storeSettings() {
        return settings;
    }

    @Override
    public void loadSettings(Properties props) { // NOSONAR
        settings = props;
    }

    @Override
    public void readData(ObjectInputStream in) throws IOException {
        channelManager.readData(in);
        // timerManager.readData(in);
    }

    @Override
    public void writeData(ObjectOutputStream out) throws IOException {
        channelManager.writeData(out);
        // timerManager.writeData(out);
    }

    @Override
    public String getMarkIconName() {
        return "tvheadend.png";
    }

    @Override
    public int getMarkPriorityForProgram(Program p) {
        return 4;
    }

    @Override
    public boolean canUseProgramTree() {
        return true;
    }

    public void markProgram(Program p) {
        p.mark(this);
    }

    public void unmarkProgram(Program p) {
        p.unmark(this);
    }

    @Override
    public void handleTvDataUpdateFinished() {
        timerManager.handleTvDataUpdateFinished();
    }

    @Override
    public void handleTvBrowserStartFinished() {
        timerManager.loadTimer();
        timerManager.mapTimerToProgram();

        epgManager.loadEpgList();
        timerTask.startThread();
    }

    public void testTVHConnection() {
        if (tvHeadendConnection.testConnection()) {
            writeLog("Connection to Tvheadend successful");
        } else {
            showError("Connection to Tvheadend failed");
        }
    }

    public void updateTimer() {
        if (tvHeadendConnection.isConnected()) {
            if (Thread.currentThread().getId() == mainThreadId) {
                timerManager.updateTimer();
            } else {
                SwingUtilities.invokeLater(() -> timerManager.updateTimer());
            }
        }
    }

    public void showError(String error) {
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(null, "Error: " + error, "Error", JOptionPane.ERROR_MESSAGE));
    }

    public void showInfo(String info) {
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(null, info, "Information", JOptionPane.INFORMATION_MESSAGE));
    }

    public void showWarning(String warning) {
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(null, "Warning: " + warning, "Warning",
                        JOptionPane.WARNING_MESSAGE));
    }

    String currentTime = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());

    public void writeLog(String log) {
        try {
            String currentTime = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            String tmpdir = System.getProperty("java.io.tmpdir");
            Files.writeString(Paths.get(tmpdir + "\\tvhbrowser.log"), currentTime + " - " + log + "\n",
                    java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}