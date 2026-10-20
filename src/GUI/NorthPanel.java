package GUI;

import DB.DbQueries;
import javax.swing.*;
import java.awt.*;

public class NorthPanel extends JPanel {
    private static boolean isAdminLoggedIn = false;

    public NorthPanel() {
        setLayout(new GridLayout(1, 1));
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(new Color(31, 31, 31));
        loginPanel.setForeground(new Color(200, 255, 200));
        loginPanel.add(new JLabel("Login Details: "));
        createLoginComponents(loginPanel);
        add(loginPanel);
    }

    public static boolean isAdmin() {
        return isAdminLoggedIn;
    }

    private void createLoginComponents(JPanel base) {
        JTextField username = new JTextField(8);
        JTextField password = new JPasswordField(8);
        JButton sub = new JButton("Submit");
        JButton exitBtn = new JButton("Vehicle Exit");

        sub.addActionListener(e -> {
            String user = username.getText();
            String pass = password.getText();
            
            new Thread(() -> {
                boolean isValid = DbQueries.validateUser(user, pass);
                SwingUtilities.invokeLater(() -> {
                    if (isValid) {
                        isAdminLoggedIn = "admin".equalsIgnoreCase(user);
                        JOptionPane.showMessageDialog(this, "Login Successful!" + (isAdminLoggedIn ? " (Admin Mode)" : ""));
                    } else {
                        isAdminLoggedIn = false;
                        JOptionPane.showMessageDialog(this, "Login Failed!");
                    }
                });
            }).start();
        });

        exitBtn.addActionListener(e -> {
            String vehicleNo = JOptionPane.showInputDialog(this, "Enter Vehicle Number for Exit:");
            if (vehicleNo != null && !vehicleNo.isEmpty()) {
                new Thread(() -> {
                    DbQueries.ParkingInfo info = DbQueries.releaseParking(vehicleNo);
                    SwingUtilities.invokeLater(() -> {
                        if (info != null) {
                            java.time.LocalDateTime outTime = java.time.LocalDateTime.now();
                            long minutes = java.time.Duration.between(info.inTime, outTime).toMinutes();
                            double fare = Math.max(1.0, minutes * 0.5); // Minimum $1, or $0.5 per minute
                            String receipt = ReceiptPrinter.generateExitReceipt(info.id, info.name, info.vehicleNo, info.inTime, outTime, fare);
                            ReceiptPrinter.showReceipt("Exit Receipt", receipt);
                            
                            // Trigger refresh in GUI if possible
                            Window win = SwingUtilities.getWindowAncestor(this);
                            if (win instanceof GUI) {
                                ((GUI) win).refreshAll();
                            }
                            
                            JOptionPane.showMessageDialog(this, "Vehicle " + vehicleNo + " Exited. Fare: $" + String.format("%.2f", fare));
                        } else {
                            JOptionPane.showMessageDialog(this, "Vehicle Number not found or not reserved.");
                        }
                    });
                }).start();
            }
        });

        base.setLayout(new FlowLayout());
        base.add(new JLabel("Username:"));
        base.add(username);
        base.add(new JLabel("Password:"));
        base.add(password);
        base.add(sub);
        base.add(new JSeparator(SwingConstants.VERTICAL));
        base.add(exitBtn);
    }

    public static void showSettingsDialog(Component parent) {
        if (isAdmin()) {
            String[] options = {"Database Settings", "Create Parking Zone", "Cancel"};
            int choice = JOptionPane.showOptionDialog(parent, "Admin Settings", "Admin",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                showDbSettings(parent);
            } else if (choice == 1) {
                showCreateZoneDialog(parent);
            }
        } else {
            showDbSettings(parent);
        }
    }

    private static void showDbSettings(Component parent) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField urlField = new JTextField("localhost");
        JTextField portField = new JTextField("3306");
        JTextField userField = new JTextField("root");
        JTextField passField = new JPasswordField();

        panel.add(new JLabel("DB URL:")); panel.add(urlField);
        panel.add(new JLabel("DB Port:")); panel.add(portField);
        panel.add(new JLabel("DB User:")); panel.add(userField);
        panel.add(new JLabel("DB Pass:")); panel.add(passField);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Database Settings", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            DbQueries.updateDbSettings(urlField.getText(), portField.getText(), userField.getText(), passField.getText());
            JOptionPane.showMessageDialog(parent, "Settings updated (Log printed to console).");
        }
    }

    private static void showCreateZoneDialog(Component parent) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField zoneNameField = new JTextField();
        JSpinner spotCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));

        panel.add(new JLabel("Zone Name:"));
        panel.add(zoneNameField);
        panel.add(new JLabel("Number of Spots:"));
        panel.add(spotCountSpinner);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Create New Parking Zone", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String zoneName = zoneNameField.getText().trim();
            int count = (int) spotCountSpinner.getValue();

            if (zoneName.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Zone name cannot be empty.");
                return;
            }

            new Thread(() -> {
                boolean success = DbQueries.createZone(zoneName, count);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(parent, "Zone '" + zoneName + "' created successfully!");
                        // Refresh GUI
                        Window win = SwingUtilities.getWindowAncestor(parent);
                        if (win instanceof GUI) {
                            ((GUI) win).loadZones();
                        }
                    } else {
                        JOptionPane.showMessageDialog(parent, "Failed to create zone. It might already exist.");
                    }
                });
            }).start();
        }
    }
}
