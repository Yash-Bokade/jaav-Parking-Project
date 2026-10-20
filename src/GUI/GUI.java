package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JPanel north, south;

    public GUI() {
        super("Parking Reservation System (Tabbed)");
        setSize(900, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(60, 63, 65));
        
        setLocationRelativeTo(null);
    }

    public void now_show() {
        setVisible(true);
    }

    public void CreateComponents() {
        north = new VehicleInPanel(this);
        north.setPreferredSize(new Dimension(800, 200));

        south = new VehicleOutPanel(this);
        south.setPreferredSize(new Dimension(800, 250));

        // Background for tabbedPane to match image frame
        tabbedPane.setBackground(new Color(60, 63, 65));
        
        // Add settings button to the north panel or a dedicated toolbar
        JButton settingsBtn = new JButton("Settings ⚙");
        settingsBtn.addActionListener(e -> NorthPanel.showSettingsDialog(this));
        
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setBackground(new Color(60, 63, 65));
        toolbar.add(settingsBtn);
        
        add(north, BorderLayout.NORTH);
        
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(toolbar, BorderLayout.NORTH);
        centerContainer.add(tabbedPane, BorderLayout.CENTER);
        
        add(centerContainer, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // Add Tabs from DB
        loadZones();

        // Auto-reload every 1 second as requested - only for selected tab
        Timer autoReloadTimer = new Timer(1000, e -> refreshSelectedTab());
        autoReloadTimer.start();
    }

    public void refreshSelectedTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            Component comp = tabbedPane.getComponentAt(selectedIndex);
            if (comp instanceof InternalParkingFrame) {
                ((InternalParkingFrame) comp).refreshParkingStatus();
            }
        }
    }

    public void loadZones() {
        new Thread(() -> {
            java.util.List<String> zones = DB.DbQueries.getAllZones();
            SwingUtilities.invokeLater(() -> {
                // To avoid mismatch, we check if zones actually changed or just refresh existing tabs
                // But for now, let's just ensure we don't remove all tabs if they are the same
                if (tabbedPane.getTabCount() == zones.size()) {
                    boolean match = true;
                    for (int i = 0; i < zones.size(); i++) {
                        if (!zones.get(i).equals(tabbedPane.getTitleAt(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        refreshAll();
                        return;
                    }
                }

                tabbedPane.removeAll();
                for (String zone : zones) {
                    addParkingTab(zone);
                }
            });
        }).start();
    }

    public void addParkingTab(String zoneName) {
        InternalParkingFrame panel = new InternalParkingFrame(zoneName);
        tabbedPane.addTab(zoneName, panel);
    }

    public void refreshAll() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof InternalParkingFrame) {
                ((InternalParkingFrame) comp).refreshParkingStatus();
            }
        }
    }
}