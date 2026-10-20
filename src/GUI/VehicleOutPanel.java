package GUI;

import DB.DbQueries;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VehicleOutPanel extends JPanel {
    private JTextField vehicleNoField;
    private JTextField parkingNoField;
    private JComboBox<String> typeBox1;
    private JComboBox<String> typeBox2;
    private JLabel inTimeLabel;
    private JLabel outTimeLabel;

    public VehicleOutPanel(GUI mainGui) {
        setBackground(new Color(220, 220, 220));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.WHITE, 10));

        JLabel titleLabel = new JLabel("Vehicle Out Details", JLabel.CENTER);
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        titleLabel.setPreferredSize(new Dimension(200, 100));
        add(titleLabel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);

        // Row 1
        JLabel vNoLabel = new JLabel("Vehicle Number");
        vNoLabel.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(vNoLabel, gbc);

        vehicleNoField = new JTextField(15);
        vehicleNoField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 0;
        centerPanel.add(vehicleNoField, gbc);

        // Row 2
        JLabel orLabel = new JLabel("Or");
        orLabel.setFont(new Font("Monospaced", Font.ITALIC, 14));
        orLabel.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 1;
        centerPanel.add(orLabel, gbc);

        typeBox1 = new JComboBox<>(new String[]{"CAR", "MOTORCYCLE", "TRUCK", "OTHER"});
        typeBox1.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 1;
        centerPanel.add(typeBox1, gbc);

        // Row 3
        JLabel pNoLabel = new JLabel("Parking Number");
        pNoLabel.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 2;
        centerPanel.add(pNoLabel, gbc);

        parkingNoField = new JTextField(15);
        parkingNoField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 2;
        centerPanel.add(parkingNoField, gbc);

        // Row 4
        typeBox2 = new JComboBox<>(new String[]{"CAR", "MOTORCYCLE", "TRUCK", "OTHER"});
        typeBox2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 3;
        centerPanel.add(typeBox2, gbc);

        add(centerPanel, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel(new GridBagLayout());
        eastPanel.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 10);
        gbc.anchor = GridBagConstraints.EAST;

        JLabel inTimeTag = new JLabel("[In Time]");
        inTimeTag.setFont(new Font("Monospaced", Font.PLAIN, 10));
        gbc.gridx = 0; gbc.gridy = 0;
        eastPanel.add(inTimeTag, gbc);

        inTimeLabel = new JLabel("00:00");
        inTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        gbc.gridy = 1;
        eastPanel.add(inTimeLabel, gbc);

        JLabel outTimeTag = new JLabel("[Out Time]");
        outTimeTag.setFont(new Font("Monospaced", Font.PLAIN, 10));
        gbc.gridy = 2;
        eastPanel.add(outTimeTag, gbc);

        outTimeLabel = new JLabel("00:00");
        outTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        gbc.gridy = 3;
        eastPanel.add(outTimeLabel, gbc);

        JButton exitBtn = new JButton("Exit");
        exitBtn.setBackground(new Color(255, 153, 153)); // Light Red
        exitBtn.setFont(new Font("Monospaced", Font.BOLD, 16));
        exitBtn.setPreferredSize(new Dimension(100, 40));
        gbc.gridy = 4; gbc.anchor = GridBagConstraints.CENTER;
        exitBtn.addActionListener(e -> exitVehicle(mainGui));
        eastPanel.add(exitBtn, gbc);

        add(eastPanel, BorderLayout.EAST);

        // Timer for Out Time
        Timer timer = new Timer(1000, e -> {
            outTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            updateInTime();
        });
        timer.start();
    }

    private void updateInTime() {
        String vNo = vehicleNoField.getText();
        String pNo = parkingNoField.getText();
        
        new Thread(() -> {
            DbQueries.ParkingInfo info = null;
            if (!vNo.isEmpty()) {
                // Find by vehicle no
                for (DbQueries.ParkingInfo p : DbQueries.getAllParkings()) {
                    if (vNo.equalsIgnoreCase(p.vehicleNo)) {
                        info = p;
                        break;
                    }
                }
            } else if (!pNo.isEmpty()) {
                try {
                    int id = Integer.parseInt(pNo);
                    info = DbQueries.getParkingById(id);
                } catch (NumberFormatException ignored) {}
            }
            
            final DbQueries.ParkingInfo finalInfo = info;
            SwingUtilities.invokeLater(() -> {
                if (finalInfo != null && finalInfo.inTime != null) {
                    inTimeLabel.setText(finalInfo.inTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                } else {
                    inTimeLabel.setText("00:00");
                }
            });
        }).start();
    }

    private void exitVehicle(GUI mainGui) {
        String vNo = vehicleNoField.getText();
        String pNo = parkingNoField.getText();

        new Thread(() -> {
            String targetVehicleNo = vNo;
            if (targetVehicleNo.isEmpty() && !pNo.isEmpty()) {
                try {
                    int id = Integer.parseInt(pNo);
                    DbQueries.ParkingInfo p = DbQueries.getParkingById(id);
                    if (p != null) targetVehicleNo = p.vehicleNo;
                } catch (NumberFormatException ignored) {}
            }

            if (targetVehicleNo == null || targetVehicleNo.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Enter Vehicle Number or Parking ID."));
                return;
            }

            DbQueries.ParkingInfo info = DbQueries.releaseParking(targetVehicleNo);
            if (info != null) {
                LocalDateTime outTime = LocalDateTime.now();
                long minutes = java.time.Duration.between(info.inTime, outTime).toMinutes();
                double fare = Math.max(1.0, minutes * 0.5);
                String receipt = ReceiptPrinter.generateExitReceipt(info.id, info.name, info.vehicleNo, info.inTime, outTime, fare);
                SwingUtilities.invokeLater(() -> {
                    ReceiptPrinter.showReceipt("Exit Receipt", receipt);
                    mainGui.refreshAll();
                    vehicleNoField.setText("");
                    parkingNoField.setText("");
                });
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Vehicle not found."));
            }
        }).start();
    }
}
