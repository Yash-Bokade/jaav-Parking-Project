package GUI;

import javax.swing.*;
import java.awt.*;

public class ReceiptPrinter {
    public static void showReceipt(String title, String content) {
        JTextArea textArea = new JTextArea(15, 30);
        textArea.setText(content);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JOptionPane.showMessageDialog(null, new JScrollPane(textArea), title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static String generateReservationReceipt(int id, String name, String vehicleNo, String vehicleType, java.time.LocalDateTime inTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("      PARKING RESERVATION RECEIPT\n");
        sb.append("------------------------------------------\n");
        sb.append("Spot ID    : ").append(id).append("\n");
        sb.append("Spot Name  : ").append(name).append("\n");
        sb.append("Vehicle No : ").append(vehicleNo).append("\n");
        sb.append("Vehicle Type: ").append(vehicleType).append("\n");
        sb.append("In Time    : ").append(inTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("        PLEASE KEEP THIS RECEIPT\n");
        return sb.toString();
    }

    public static String generateExitReceipt(int id, String name, String vehicleNo, java.time.LocalDateTime inTime, java.time.LocalDateTime outTime, double fare) {
        StringBuilder sb = new StringBuilder();
        sb.append("         PARKING EXIT RECEIPT\n");
        sb.append("------------------------------------------\n");
        sb.append("Spot Name  : ").append(name).append("\n");
        sb.append("Vehicle No : ").append(vehicleNo).append("\n");
        sb.append("In Time    : ").append(inTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Out Time   : ").append(outTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Duration   : ").append(java.time.Duration.between(inTime, outTime).toMinutes()).append(" minutes\n");
        sb.append("------------------------------------------\n");
        sb.append("TOTAL FARE : $").append(String.format("%.2f", fare)).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("        THANK YOU FOR VISITING!\n");
        return sb.toString();
    }
}
