package GUI;

import DB.DbQueries;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VehicleInPanel extends JPanel {
    private JTextField vehicleNoField;
    private JComboBox<String> typeBox;
    private JLabel timeLabel;

    public VehicleInPanel(GUI mainGui) {
        setBackground(new Color(220, 220, 220));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 10));

        JLabel titleLabel = new JLabel("Vehicle In Details", JLabel.CENTER);
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        titleLabel.setPreferredSize(new Dimension(200, 100));
        add(titleLabel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel vNoLabel = new JLabel("Vehicle Number");
        vNoLabel.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(vNoLabel, gbc);

        vehicleNoField = new JTextField(15);
        vehicleNoField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 0;
        centerPanel.add(vehicleNoField, gbc);

        typeBox = new JComboBox<>(new String[]{"CAR", "MOTORCYCLE", "TRUCK", "OTHER"});
        typeBox.setFont(new Font("Monospaced", Font.PLAIN, 16));
        typeBox.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 1;
        centerPanel.add(typeBox, gbc);

        add(centerPanel, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel(new GridBagLayout());
        eastPanel.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);

        timeLabel = new JLabel("00:00");
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0;
        eastPanel.add(timeLabel, gbc);
        
        JLabel timeTag = new JLabel("[Time]");
        timeTag.setFont(new Font("Monospaced", Font.PLAIN, 10));
        gbc.gridy = 0; gbc.anchor = GridBagConstraints.NORTHEAST;
        eastPanel.add(timeTag, gbc);

        JButton parkBtn = new JButton("Park");
        parkBtn.setBackground(new Color(144, 238, 144)); // Light Green
        parkBtn.setFont(new Font("Monospaced", Font.BOLD, 16));
        parkBtn.setPreferredSize(new Dimension(100, 40));
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.CENTER;
        parkBtn.addActionListener(e -> parkVehicle(mainGui));
        eastPanel.add(parkBtn, gbc);

        add(eastPanel, BorderLayout.EAST);

        // Update time
        Timer timer = new Timer(1000, e -> {
            timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        });
        timer.start();
    }

    private void parkVehicle(GUI mainGui) {
        String vNo = vehicleNoField.getText();
        String type = (String) typeBox.getSelectedItem();
        if (vNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter vehicle number");
            return;
        }
        
        // In the original app, we picked a spot by clicking. 
        // Here, the image shows a "Park" button. 
        // We'll find the first available spot.
        new Thread(() -> {
            java.util.List<DbQueries.ParkingInfo> parkings = DbQueries.getAllParkings();
            int targetId = -1;
            String targetName = "";
            for (DbQueries.ParkingInfo p : parkings) {
                if ("available".equalsIgnoreCase(p.status)) {
                    targetId = p.id;
                    targetName = p.name;
                    break;
                }
            }

            if (targetId != -1) {
                boolean success = DbQueries.reserveParking(targetId, vNo, type);
                final String finalName = targetName;
                final int finalId = targetId;
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        String receipt = ReceiptPrinter.generateReservationReceipt(finalId, finalName, vNo, type, LocalDateTime.now());
                        ReceiptPrinter.showReceipt("Reservation Receipt", receipt);
                        mainGui.refreshAll();
                        vehicleNoField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(this, "Parking full or failed.");
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "No available spots."));
            }
        }).start();
    }
}
