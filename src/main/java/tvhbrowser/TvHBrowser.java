/**
 * The `TvHBrowser` class is a plugin that exports program schedules from TV-Browser to Tvheadend.
 * It provides functionality for configuring settings, managing timers, and interacting with Tvheadend.
 *
 * Licensed under AGPL-3.0.
 */

package tvhbrowser;

import devplugin.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class TvHBrowser extends Plugin {

    private final static Version VERSION = new Version(0, 1, true);
    private final static PluginInfo INFO = new PluginInfo(TvHBrowser.class,
            "TVHBrowser",
            "Exports program schedules from TV-Browser to Tvheadend",
            "",
            "AGPL-3.0",
            "");

    private static final long MAX_LOG_BYTES = 5L * 1024 * 1024;
    private static final java.nio.file.Path LOG_FILE = Paths.get(System.getProperty("java.io.tmpdir"),
            "tvhbrowser.log");

    private static final util.i18n.Localizer mLocalizer = util.i18n.Localizer.getLocalizerFor(TvHBrowser.class);

    private final TimerManager timerManager;
    private final ChannelManager channelManager;
    private final TVHeadendConnection tvHeadendConnection;
    private final TVHTimerTask timerTask;
    private final ContextMenuFactory cmf;
    private final EpgManager epgManager;

    private volatile Properties settings = new Properties();

    public TvHBrowser() {
        this.tvHeadendConnection = new TVHeadendConnection(this);
        this.channelManager = new ChannelManager(this, tvHeadendConnection);
        this.epgManager = new EpgManager(this, tvHeadendConnection, channelManager);
        this.timerManager = new TimerManager(this, tvHeadendConnection, channelManager, epgManager);
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

    @Override
    public final String getPluginCategory() {
        return CATEGORY_REMOTE_CONTROL_SOFTWARE;
    }

    @Override
    public void onDeactivation() {
        timerTask.stopThread();
        timerManager.shutdown();
    }

    public static String getTranslation(String key, String altText) {
        return mLocalizer.msg(key, altText);
    }

    public static String getTranslation(String key, String altText, String arg1) {
        return mLocalizer.msg(key, altText, arg1);
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
        try {
            return Integer.parseInt(settings.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setSetting(String key, boolean value) {
        settings.setProperty(key, String.valueOf(value));
    }

    public boolean getSetting(String key, boolean defaultValue) {
        return Boolean.parseBoolean(settings.getProperty(key, String.valueOf(defaultValue)).trim());
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
        try {
            timerManager.readManualMappings(in);
        } catch (EOFException e) {
            // data written by an older version
        }
    }

    @Override
    public void writeData(ObjectOutputStream out) throws IOException {
        channelManager.writeData(out);
        timerManager.writeManualMappings(out);
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
        SwingUtilities.invokeLater(this::refreshProgramTree);
    }

    @Override
    public void handleTvBrowserStartFinished() {
        channelManager.getTVBChannelList();
        timerManager.updateTimer();

        Thread epgLoader = new Thread(() -> {
            channelManager.getTVHeadendChannels();
            epgManager.loadEpgList();
        }, "tvh-epg-loader");
        epgLoader.setDaemon(true);
        epgLoader.start();

        timerTask.startThread();
    }

    public void testTVHConnection() {
        Thread tester = new Thread(() -> {
            if (tvHeadendConnection.testConnection()) {
                writeLog("Connection to Tvheadend successful");
                timerManager.updateTimer();
            }
        }, "tvh-connection-test");
        tester.setDaemon(true);
        tester.start();
    }

    public void updateTimer() {
        timerManager.updateTimer();
    }

    @Override
    protected void loadRootNode(PluginTreeNode rootNode) {
        if (rootNode == null) {
            return;
        }

        rootNode.removeAllChildren();
        addTimerNodes(rootNode.addNode(getTranslation("upcomingRecordings", "Anstehende Aufnahmen")),
                timerManager.getUpcomingTimers());
        addTimerNodes(rootNode.addNode(getTranslation("finishedRecordings", "Abgeschlossene Aufnahmen")),
                timerManager.getFinishedTimers());
        rootNode.update();
    }

    private void addTimerNodes(PluginTreeNode parent, List<Timer> timers) {
        Map<String, PluginTreeNode> dayNodes = new LinkedHashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Timer timer : timers) {
            String dayLabel = timer.getDate().getShortDayLongMonthString();
            PluginTreeNode dayNode = dayNodes.get(dayLabel);
            if (dayNode == null) {
                dayNode = parent.addNode(dayLabel);
                dayNode.setGroupingByDateEnabled(false);
                dayNodes.put(dayLabel, dayNode);
            }

            Program program = timerManager.getProgramForTimer(timer.getUuid());
            if (program != null) {
                dayNode.addProgram(program);
                continue;
            }

            String channelName = timer.getChannelname() == null ? "" : timer.getChannelname().trim();
            String label = timer.getStartDateTime().format(timeFormatter) + " - " + getDisplayTitle(timer);
            dayNode.addNode(channelName.isEmpty() ? label : label + " [" + channelName + "]");
        }
    }

    private String getDisplayTitle(Timer timer) {
        List<String> languages = new ArrayList<>();
        languages.add(Locale.getDefault().getISO3Language());
        languages.add("deu");
        languages.add("ger");
        languages.add("eng");

        for (String language : languages) {
            String title = timer.getTitle(language);
            if (title != null && !title.trim().isEmpty()) {
                return title.trim();
            }
        }
        if (timer.getTitles() != null) {
            for (String value : timer.getTitles().values()) {
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }

        return getTranslation("untitled", "Ohne Titel");
    }

    public void refreshProgramTree() {
        PluginTreeNode rootNode = getRootNode();
        loadRootNode(rootNode);
        storeRootNode();
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

    public EpgProgram chooseEpgProgram(String message, Program reference, List<EpgProgram> options) {
        AtomicReference<EpgProgram> result = new AtomicReference<>();
        AtomicBoolean failed = new AtomicBoolean();
        Runnable dialog = () -> {
            try {
                result.set(EpgChoiceDialog.show(getParentFrame(), message, reference.getChannel(), options));
            } catch (RuntimeException | LinkageError e) {
                writeLog("Program list dialog failed, using simple dialog: " + e);
                failed.set(true);
            }
            if (failed.get()) {
                result.set(EpgChoiceDialog.showSimple(message, options));
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                dialog.run();
            } else {
                SwingUtilities.invokeAndWait(dialog);
            }
        } catch (Exception e) {
            writeLog("Selection dialog failed: " + e);
        }
        return result.get();
    }

    public synchronized void writeLog(String log) {
        try {
            if (Files.exists(LOG_FILE) && Files.size(LOG_FILE) > MAX_LOG_BYTES) {
                Files.move(LOG_FILE, LOG_FILE.resolveSibling("tvhbrowser.log.old"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String currentTime = new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
            Files.writeString(LOG_FILE, currentTime + " - " + log + "\n",
                    java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
