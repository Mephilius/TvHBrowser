package tvhbrowser;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChannelPanel {
    private final JPanel panel;
    private JTable leftTable;
    private JTable rightTable;
    private final TvHBrowser tvhBrowser;
    private final ChannelManager channelManager;

    /**
     * @param tvhBrowser
     * @param channelManager
     */
    public ChannelPanel(TvHBrowser tvhBrowser, ChannelManager channelManager) {
        this.tvhBrowser = tvhBrowser;
        this.channelManager = channelManager;
        this.panel = new JPanel(new BorderLayout());

        initializeTables();
        initializeButtons();
        initializeRowColorRenderers();
        fillLeftTable();
        initializeRightTable();
        sortTables();
    }

    private void initializeTables() {
        // Initialize left table
        leftTable = new JTable();
        leftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Set selection mode to single selection
        JScrollPane leftScrollPane = new JScrollPane(leftTable);
        leftScrollPane.setPreferredSize(new Dimension(200, 300));
        panel.add(leftScrollPane, BorderLayout.WEST);

        // Initialize right table
        rightTable = new JTable();
        rightTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Set selection mode to single selection
        JScrollPane rightScrollPane = new JScrollPane(rightTable);
        rightScrollPane.setPreferredSize(new Dimension(400, 300));
        panel.add(rightScrollPane, BorderLayout.CENTER);

    }

    private void sortTables() {
        channelManager.writeLog("Sorting tables");
        TableRowSorter<DefaultTableModel> leftSorter = new TableRowSorter<DefaultTableModel>(
                (DefaultTableModel) leftTable.getModel());
        leftSorter.setSortable(0, true);
        leftTable.setRowSorter(leftSorter);
        TableRowSorter<DefaultTableModel> rightSorter = new TableRowSorter<DefaultTableModel>(
                (DefaultTableModel) rightTable.getModel());
        rightSorter.setSortable(0, true);
        rightTable.setRowSorter(rightSorter);
    }

    private void initializeButtons() {
        // Add buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

        JButton moveRightButton = new JButton("map");
        moveRightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapSelectedRows();
            }
        });
        buttonPanel.add(moveRightButton);

        JButton moveLeftButton = new JButton("remove");
        moveLeftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unmmapSelectedRow();
            }
        });
        buttonPanel.add(moveLeftButton);

        JButton autoButton = new JButton("auto mapping");
        autoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoMapping();
            }
        });
        // Implement Auto button functionality if needed
        buttonPanel.add(autoButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initializeRowColorRenderers() {
        // Add row color renderer to left table
        leftTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                        row, column);
                if (!isSelected) {
                    if (row % 2 == 0) {
                        rendererComponent.setBackground(Color.LIGHT_GRAY);
                    } else {
                        rendererComponent.setBackground(Color.WHITE);
                    }
                }
                return rendererComponent;
            }
        });

        // Add row color renderer to right table
        rightTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                        row, column);
                if (!isSelected) {
                    if (row % 2 == 0) {
                        rendererComponent.setBackground(Color.LIGHT_GRAY);
                    } else {
                        rendererComponent.setBackground(Color.WHITE);
                    }
                }
                return rendererComponent;
            }
        });
    }

    private void initializeRightTable() {
        // Initialize right table with TVB channel names
        Collection<devplugin.Channel> tvbChannels = channelManager.getTVBChannelList();
        DefaultTableModel rightTableModel = new DefaultTableModel(
                new Object[] { "TV Browser Channel Names", "TVHeadend Channel Names" }, 0);
        for (devplugin.Channel channel : tvbChannels) {
            TVHeadendChannel tvhChannel = channelManager.getMappedTvhChannel(channel.getUniqueId());
            if (tvhChannel != null) {
                rightTableModel.addRow(new Object[] { channel.getDefaultName(), tvhChannel.getVal() });
            } else {
                rightTableModel.addRow(new Object[] { channel.getDefaultName(), "" });
            }
        }

        rightTable.setModel(rightTableModel);

    }

    // Rest of the methods...

    public JPanel getPanel() {
        channelManager.importChannelLists();
        return panel;
    }

    private void mapSelectedRows() {
        DefaultTableModel leftModel = (DefaultTableModel) leftTable.getModel();
        DefaultTableModel rightModel = (DefaultTableModel) rightTable.getModel();
        int selectedRow = leftTable.getSelectedRow();
        if (selectedRow != -1) { // Check if any row is selected
            String channelName = (String) leftModel.getValueAt(leftTable.convertRowIndexToModel(selectedRow), 0);
            int targetRow = rightTable.getSelectedRow(); // Get selected row in target table
            if (targetRow != -1) { // Check if any row is selected in the target table
                int modelTargetRow = rightTable.convertRowIndexToModel(targetRow);
                String existingText = (String) rightModel.getValueAt(modelTargetRow, 1);
                if (existingText.isEmpty()) {
                    channelManager.mapChannelsByName(rightModel.getValueAt(modelTargetRow, 0).toString(), channelName);
                    rightModel.setValueAt(channelName, modelTargetRow, 1);
                } else {
                    this.tvhBrowser.showInfo("Is already mapped with " + existingText);
                }

                // leftModel.removeRow(selectedRow);
            }
        }

        cleanAndFillLeftTable();
    }

    private boolean compare(String text1, String text2) {
        // Remove "HD" from the end of text
        text1 = text1.replaceAll("\\bHD$", "").trim().toLowerCase();
        text2 = text2.replaceAll("\\bHD$", "").trim().toLowerCase();

        // Split text into words
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");

        // Check if text1 exactly matches text2
        if (text1.equals(text2)) {
            return true;
        }

        // Check if at least two words from text1 are contained in text2
        if (words1.length == 1 && words2.length > 1) {
            for (String word : words2) {
                if (text1.equals(word)) {
                    return true;
                }
            }
        } else {
            int count = 0;
            for (String word : words1) {
                if (text2.contains(word)) {
                    count++;
                }
            }
            if (count >= 2) {
                return true;
            }
        }

        return false;
    }

    private double compareScore(String text1, String text2) {
        String normalized1 = normalize(text1); // nur lowercase + trim
        String normalized2 = normalize(text2);

        if (normalized1.equals(normalized2))
            return 1.0;

        String noHd1 = normalizeWithoutHD(text1); // lowercase + trim + HD weg
        String noHd2 = normalizeWithoutHD(text2);

        if (noHd1.equals(noHd2))
            return 0.9;

        Set<String> words1 = new HashSet<>(Arrays.asList(noHd1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(noHd2.split("\\s+")));

        Set<String> common = new HashSet<>(words1);
        common.retainAll(words2);

        int total = Math.max(words1.size(), words2.size());
        if (total == 0)
            return 0.0;

        return (double) common.size() / total * 0.8;
    }

    private String normalize(String text) {
        return text.trim().toLowerCase(); // nur einfache Normalisierung
    }

    private String normalizeWithoutHD(String text) {
        return text
                .replaceAll("(?i)\\bHD$", "")
                .trim()
                .toLowerCase();
    }


    private void autoMapping() {
        DefaultTableModel leftTableModel = (DefaultTableModel) leftTable.getModel();
        DefaultTableModel rightTableModel = (DefaultTableModel) rightTable.getModel();

        for (int i = 0; i < leftTableModel.getRowCount(); i++) {
            String leftText = (String) leftTableModel.getValueAt(i, 0);
            if (leftText == null)
                continue;

            int bestMatchIndex = -1;
            double bestScore = 0.0;

            for (int j = 0; j < rightTableModel.getRowCount(); j++) {
                String rightText = (String) rightTableModel.getValueAt(j, 0);
                String rightMapped = (String) rightTableModel.getValueAt(j, 1);

                if (rightText == null || (rightMapped != null && !rightMapped.isEmpty())) {
                    continue; // überspringen wenn schon zugeordnet
                }

                double score = compareScore(leftText, rightText);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatchIndex = j;
                }
            }

            // Mindest-Schwelle (z. B. 0.5) um irrelevante Mappings zu vermeiden
            if (bestMatchIndex != -1 && bestScore >= 0.5) {
                String bestRightText = (String) rightTableModel.getValueAt(bestMatchIndex, 0);
                rightTableModel.setValueAt(leftText, bestMatchIndex, 1);
                channelManager.mapChannelsByName(bestRightText, leftText);
                leftTableModel.removeRow(i);
                i--; // wegen Row-Shift nach remove
            }
        }

        cleanAndFillLeftTable();
    }

    private void unmmapSelectedRow() {
        int selectedRow = rightTable.getSelectedRow();
        if (selectedRow != -1) {

            channelManager.writeLog("Unmapping selected row " + selectedRow);
            // DefaultTableModel leftTableModel = (DefaultTableModel) leftTable.getModel();
            DefaultTableModel rightTableModel = (DefaultTableModel) rightTable.getModel();

            int modelIndex = rightTable.convertRowIndexToModel(selectedRow);
            // Get the text from the right table
            String tvhText = (String) rightTableModel.getValueAt(modelIndex, 1);

            String tvbText = (String) rightTableModel.getValueAt(modelIndex, 0);

            // Add the text to the left table
            // leftTableModel.addRow(new Object[] { rightText });

            // Remove the text from the right table
            rightTableModel.setValueAt("", modelIndex, 1);

            // Remove the mapping from the ChannelManager
            channelManager.writeLog("Unmapping " + tvhText + " from " + tvbText);
            channelManager.unmapChannelsByName(tvbText, tvhText);

        }

        cleanAndFillLeftTable();
    }

    private void fillLeftTable() {
        DefaultTableModel leftTableModel = (DefaultTableModel) leftTable.getModel();
        if (leftTableModel.getRowCount() == 0) {
            leftTableModel = new DefaultTableModel(new Object[] { "TVHeadend Channels" }, 0);
            leftTable.setModel(leftTableModel);
        } else {
            leftTableModel.setRowCount(0);
        }

        Collection<TVHeadendChannel> tvhChannels = channelManager.getUnmappedTVHChannels();
        for (TVHeadendChannel channel : tvhChannels) {
            leftTableModel.addRow(new Object[] { channel.getVal() });
        }
    }

    private void cleanAndFillLeftTable() {
        DefaultTableModel leftTableModel = (DefaultTableModel) leftTable.getModel();
        leftTableModel.setRowCount(0);

        Collection<TVHeadendChannel> unmappedChannels = channelManager.getUnmappedTVHChannels();
        for (TVHeadendChannel channel : unmappedChannels) {
            leftTableModel.addRow(new Object[] { channel.getVal() });
        }
    }

}
