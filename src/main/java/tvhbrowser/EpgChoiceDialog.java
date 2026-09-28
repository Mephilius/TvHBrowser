package tvhbrowser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import devplugin.Channel;
import devplugin.Program;
import util.i18n.Localizer;
import util.ui.ProgramList;

/** Lets the user pick one of several Tvheadend events. Must be called on the Swing thread. */
final class EpgChoiceDialog {
    private static final int MAX_REFRESHES = 4;

    private EpgChoiceDialog() {
    }

    static String title() {
        return TvHBrowser.getTranslation("chooseTitle", "Tvheadend-Sendung wählen");
    }

    static EpgProgram show(Frame parent, String message, Channel channel, List<EpgProgram> options) {
        DefaultListModel<Program> model = new DefaultListModel<>();
        ProgramList list = new ProgramList(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JDialog dialog = new JDialog(parent, title(), true);
        JButton ok = new JButton(Localizer.getLocalization(Localizer.I18N_OK));
        JButton cancel = new JButton(Localizer.getLocalization(Localizer.I18N_CANCEL));
        EpgProgram[] chosen = new EpgProgram[1];

        Runnable accept = () -> {
            Program selected = list.getSelectedValue();
            if (selected instanceof EpgChoiceProgram) {
                chosen[0] = ((EpgChoiceProgram) selected).getEpgProgram();
            }
            dialog.dispose();
        };
        ok.addActionListener(e -> accept.run());
        cancel.addActionListener(e -> dialog.dispose());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    accept.run();
                }
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);
        dialog.getContentPane().setLayout(new BorderLayout(5, 5));
        dialog.getContentPane().add(new JLabel("<html>" + message.replace("\n", "<br>") + "</html>"),
                BorderLayout.NORTH);
        dialog.getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.setSize(800, Math.min(700, 200 + options.size() * 160));
        dialog.validate();

        // ProgramList computes its row heights from the current layout, so the rows are added and refreshed
        // after the dialog has its final size
        for (EpgProgram e : options) {
            model.addElement(new EpgChoiceProgram(channel, e));
        }
        list.setSelectedIndex(0);
        dialog.validate();

        int[] refreshes = { 0 };
        Runnable refresh = () -> {
            if (refreshes[0]++ >= MAX_REFRESHES) {
                return;
            }
            for (int i = 0; i < model.size(); i++) {
                model.setElementAt(model.get(i), i);
            }
            list.setSelectedIndex(Math.max(0, list.getSelectedIndex()));
            list.revalidate();
            list.repaint();
        };
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(refresh);
            }
        });
        list.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(refresh);
            }
        });
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return chosen[0];
    }

    static EpgProgram showSimple(String message, List<EpgProgram> options) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
        Object[] choices = new Object[options.size()];
        for (int i = 0; i < choices.length; i++) {
            EpgProgram e = options.get(i);
            String subtitle = e.getSubtitle() == null || e.getSubtitle().isBlank() ? "" : " - " + e.getSubtitle();
            String label = e.getStartDateTime().format(timeFormatter) + "  " + e.getTitle() + subtitle;
            choices[i] = new Object() {
                @Override
                public String toString() {
                    return label;
                }
            };
        }

        Object selected = JOptionPane.showInputDialog(null, message, title(), JOptionPane.QUESTION_MESSAGE, null,
                choices, choices[0]);
        for (int i = 0; i < choices.length; i++) {
            if (choices[i] == selected) {
                return options.get(i);
            }
        }
        return null;
    }
}
