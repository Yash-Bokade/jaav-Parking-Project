package GUI;

import DB.DbQueries;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InternalParkingFrame extends JPanel {
    private JPanel parkingPanel;
    private JLabel leftLabel;
    private String zoneName;

    public InternalParkingFrame(String zoneName) {
        this.zoneName = zoneName;
        // super(title, true, true, true, true); // Removed JInternalFrame constructor
        // setSize(700, 400); // Handled by layout/parent
        setLayout(new BorderLayout());
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setPreferredSize(new Dimension(180, 0));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Color.LIGHT_GRAY));
        
        leftLabel = new JLabel("<html>Parking<br>Space<br>Details<br>...</html>");
        leftLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        leftLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        leftPanel.add(leftLabel, BorderLayout.NORTH);
        
        add(leftPanel, BorderLayout.WEST);

        parkingPanel = new JPanel(new GridLayout(0, 9, 10, 10));
        parkingPanel.setBackground(Color.WHITE);
        parkingPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        add(new JScrollPane(parkingPanel), BorderLayout.CENTER);
        refreshParkingStatus();
    }

    public void refreshParkingStatus() {
        // Use a thread to fetch from DB to avoid blocking UI
        new Thread(() -> {
            List<DbQueries.ParkingInfo> parkings = DbQueries.getParkingsByZone(zoneName);
            SwingUtilities.invokeLater(() -> {
                // If the number of components changed or it's empty, rebuild
                if (parkingPanel.getComponentCount() != parkings.size()) {
                    parkingPanel.removeAll();
                    leftLabel.setText("<html>Parking<br>Space<br>Details<br>...</html>");
                    for (DbQueries.ParkingInfo p : parkings) {
                        parkingPanel.add(createSpotBox(p));
                    }
                    parkingPanel.revalidate();
                    parkingPanel.repaint();
                } else {
                    // Update existing components to avoid UI flickering and losing selection
                    for (int i = 0; i < parkings.size(); i++) {
                        updateSpotBox(parkingPanel.getComponent(i), parkings.get(i));
                    }
                }
            });
        }).start();
    }

    private JPanel createSpotBox(DbQueries.ParkingInfo p) {
        JPanel spotBox = new JPanel(new BorderLayout());
        spotBox.setOpaque(false);
        
        JButton spot = new JButton();
        spot.setPreferredSize(new Dimension(50, 70));
        
        JLabel nameLabel = new JLabel(p.name, JLabel.CENTER);
        nameLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        
        spotBox.add(spot, BorderLayout.CENTER);
        spotBox.add(nameLabel, BorderLayout.SOUTH);
        
        updateSpotBox(spotBox, p);
        return spotBox;
    }

    private void updateSpotBox(Component comp, DbQueries.ParkingInfo p) {
        if (!(comp instanceof JPanel)) return;
        JPanel spotBox = (JPanel) comp;
        JButton spot = (JButton) spotBox.getComponent(0);
        
        // Remove old action listeners to avoid duplicates
        for (java.awt.event.ActionListener al : spot.getActionListeners()) {
            spot.removeActionListener(al);
        }

        if ("reserved".equalsIgnoreCase(p.status)) {
            spot.setBackground(new Color(255, 102, 102)); // Reddish
        } else {
            // Keep green if it was selected locally (though server state might differ)
            // For now, reset to LIGHT_GRAY if not reserved, unless it's the currently selected one in the details
            if (!spot.getBackground().equals(Color.GREEN)) {
                spot.setBackground(Color.LIGHT_GRAY);
            }
        }

        spot.addActionListener(e -> {
            if (!"reserved".equalsIgnoreCase(p.status)) {
                // Toggle green (selected)
                if (spot.getBackground().equals(Color.GREEN)) {
                    spot.setBackground(Color.LIGHT_GRAY);
                    leftLabel.setText("<html>Parking<br>Space<br>Details<br>...</html>");
                } else {
                    spot.setBackground(Color.GREEN);
                    leftLabel.setText("<html>Spot: " + p.name + "<br>Status: Selected</html>");
                    showReservationForm(p.id, p.name);
                }
            } else {
                String details = "<html>Spot: " + p.name + "<br>";
                details += "Status: Reserved<br>";
                details += "Vehicle: " + (p.vehicleNo != null ? p.vehicleNo : "N/A") + "<br>";
                if (p.inTime != null) {
                    details += "At: " + p.inTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                }
                details += "</html>";
                leftLabel.setText(details);
            }
        });
    }

    private void showReservationForm(int id, String name) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField vehicleNoField = new JTextField();
        String[] types = {"Car", "Motorcycle", "Truck", "Other"};
        JComboBox<String> typeBox = new JComboBox<>(types);

        panel.add(new JLabel("Vehicle No:"));
        panel.add(vehicleNoField);
        panel.add(new JLabel("Vehicle Type:"));
        panel.add(typeBox);

        int result = JOptionPane.showConfirmDialog(null, panel, "Reserve Spot " + name, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String vehicleNo = vehicleNoField.getText();
            String type = (String) typeBox.getSelectedItem();
            if (vehicleNo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vehicle number is required.");
                return;
            }

            new Thread(() -> {
                boolean success = DbQueries.reserveParking(id, vehicleNo, type);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        String receipt = ReceiptPrinter.generateReservationReceipt(id, name, vehicleNo, type, now);
                        ReceiptPrinter.showReceipt("Reservation Receipt", receipt);
                        refreshParkingStatus();
                    } else {
                        JOptionPane.showMessageDialog(this, "Reservation failed. Spot might have been taken.");
                    }
                });
            }).start();
        }
    }

    private void updateStatus(int id, String status) {
        new Thread(() -> {
            DbQueries.updateParkingStatus(id, status);
        }).start();
    }
}
