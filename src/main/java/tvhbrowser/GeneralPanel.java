package tvhbrowser;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

import tvhbrowser.*;

public class GeneralPanel {
    private JPanel panel;
    private JTextField fqdnField;
    private JSpinner portSpinner;
    private JSpinner timeoutSpinner;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox httpsCheckBox;
    private TvHBrowser tvhBrowser;

    public GeneralPanel(TvHBrowser tvhBrowser) {
        this.tvhBrowser = tvhBrowser;
        initComponents();
        loadSettings();
    }

    public void onClose() {
        tvhBrowser.testTVHConnection();
    }

    private void initComponents() {
        panel = new JPanel(new BorderLayout());

        panel.add(new JLabel("Allgemein"), BorderLayout.CENTER);

        // Einstellungen Abschnitt
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Einstellungen"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // TVHeadende FQDN / IP
        JLabel fqdnLabel = new JLabel("TVHeadend FQDN / IP:");
        settingsPanel.add(fqdnLabel, gbc);

        gbc.gridx++;
        fqdnField = new JTextField("tvheadend.local", 15);
        fqdnField.setPreferredSize(new Dimension(200, fqdnField.getPreferredSize().height));
        settingsPanel.add(fqdnField, gbc);

        // Port
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel portLabel = new JLabel("Port:");
        settingsPanel.add(portLabel, gbc);

        gbc.gridx++;
        SpinnerNumberModel portModel = new SpinnerNumberModel(9981, 0, Integer.MAX_VALUE, 1);
        portSpinner = new JSpinner(portModel);
        JSpinner.NumberEditor portEditor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(portEditor);
        portSpinner.setPreferredSize(new Dimension(120, portSpinner.getPreferredSize().height));
        settingsPanel.add(portSpinner, gbc);

        // Timeout
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel timeoutLabel = new JLabel("Timeout:");
        settingsPanel.add(timeoutLabel, gbc);

        gbc.gridx++;
        SpinnerNumberModel timeoutModel = new SpinnerNumberModel(500, 0, Integer.MAX_VALUE, 100);
        timeoutSpinner = new JSpinner(timeoutModel);
        JSpinner.NumberEditor timeoutEditor = new JSpinner.NumberEditor(timeoutSpinner, "#");
        timeoutSpinner.setEditor(timeoutEditor);
        timeoutSpinner.setPreferredSize(new Dimension(120, timeoutSpinner.getPreferredSize().height));
        settingsPanel.add(timeoutSpinner, gbc);

        // Https connection
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel httpsLabel = new JLabel("Https connection:");
        settingsPanel.add(httpsLabel, gbc);

        gbc.gridx++;
        httpsCheckBox = new JCheckBox();
        settingsPanel.add(httpsCheckBox, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel usernameLabel = new JLabel("Username:");
        settingsPanel.add(usernameLabel, gbc);

        gbc.gridx++;
        usernameField = new JTextField(15);
        usernameField.setPreferredSize(new Dimension(200, usernameField.getPreferredSize().height));
        settingsPanel.add(usernameField, gbc);

        // Passwort
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Passwort:");
        settingsPanel.add(passwordLabel, gbc);

        gbc.gridx++;
        passwordField = new JPasswordField(15);
        passwordField.setPreferredSize(new Dimension(200, passwordField.getPreferredSize().height));
        settingsPanel.add(passwordField, gbc);

        panel.add(settingsPanel, BorderLayout.NORTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    private void loadSettings() {
        fqdnField.setText(tvhBrowser.getSetting("fqdn", "tvheadend.local"));
        portSpinner.setValue(tvhBrowser.getSetting("port", 9981));
        timeoutSpinner.setValue(tvhBrowser.getSetting("timeout", 500));
        httpsCheckBox.setSelected(tvhBrowser.getSetting("https", "false").equals("true"));
        usernameField.setText(tvhBrowser.getSetting("username", ""));
        passwordField.setText(tvhBrowser.getSetting("password", ""));
    }

    public void saveSettings() {
        tvhBrowser.setSetting("fqdn", fqdnField.getText());
        tvhBrowser.setSetting("port", (int) portSpinner.getValue());
        tvhBrowser.setSetting("timeout", (int) timeoutSpinner.getValue());
        tvhBrowser.setSetting("https", httpsCheckBox.isSelected() ? "true" : "false");
        tvhBrowser.setSetting("username", usernameField.getText());
        tvhBrowser.setSetting("password", new String(passwordField.getPassword()));
    }
}
