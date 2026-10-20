import GUI.GUI;
import Networking.ParkingServer;
import Networking.ParkingClient;

public class Main {
    private static boolean isClient = true;

    public static boolean isClient() {
        return isClient;
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            isClient = false;
            // Start the Networking Server
            new ParkingServer().start();
        } else {
            isClient = true;
            // Connect to server (default localhost)
            ParkingClient.getInstance("127.0.0.1", 12345);

            // Launch the GUI
            javax.swing.SwingUtilities.invokeLater(() -> {
                GUI gui = new GUI();
                gui.CreateComponents();
                gui.now_show();
                
                // Register real-time update callbacks
                ParkingClient.getInstance().onRefresh(gui::refreshAll);
                ParkingClient.getInstance().onLoadZones(gui::loadZones);
            });
        }
    }
}
