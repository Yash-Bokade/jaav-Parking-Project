package Networking;

import DB.DbRepository;
import DB.DbQueries;
import java.io.*;
import java.net.*;
import java.util.*;

public class ParkingServer {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public void start() {
        DbRepository.initDatabase();
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Parking Server started on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void broadcast(String message) {
        synchronized (clients) {
            Iterator<ClientHandler> it = clients.iterator();
            while (it.hasNext()) {
                ClientHandler client = it.next();
                if (!client.sendMessage(message)) {
                    it.remove();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public boolean sendMessage(String message) {
            if (out != null) {
                out.println(message);
                return !out.checkError();
            }
            return false;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String input;
                while ((input = in.readLine()) != null) {
                    handleCommand(input);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                clients.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleCommand(String input) {
            String[] parts = input.split("\\|");
            String cmd = parts[0];
            
            try {
                switch (cmd) {
                    case "LOGIN":
                        boolean valid = DbRepository.validateUser(parts[1], parts[2]);
                        out.println("LOGIN_RES|" + valid);
                        break;
                    case "GET_ZONES":
                        List<String> zones = DbRepository.getAllZones();
                        out.println("ZONES_RES|" + String.join(",", zones));
                        break;
                    case "GET_PARKINGS_ZONE":
                        List<DbQueries.ParkingInfo> pList = DbRepository.getParkingsByZone(parts[1]);
                        out.println("PARKINGS_RES|" + serializeParkings(pList));
                        break;
                    case "RESERVE":
                        // RESERVE|id|vNo|type
                        boolean res = DbRepository.reserveParking(Integer.parseInt(parts[1]), parts[2], parts[3]);
                        out.println("RESERVE_RES|" + res);
                        if (res) broadcast("REFRESH");
                        break;
                    case "RELEASE":
                        // RELEASE|vNo
                        DbQueries.ParkingInfo info = DbRepository.releaseParking(parts[1]);
                        if (info != null) {
                            out.println("RELEASE_RES|SUCCESS|" + serializeParking(info));
                            broadcast("REFRESH");
                        } else {
                            out.println("RELEASE_RES|FAILED");
                        }
                        break;
                    case "CREATE_ZONE":
                        // CREATE_ZONE|name|count
                        boolean cz = DbRepository.createZone(parts[1], Integer.parseInt(parts[2]));
                        out.println("CREATE_ZONE_RES|" + cz);
                        if (cz) broadcast("LOAD_ZONES");
                        break;
                    case "GET_ALL_PARKINGS":
                        out.println("ALL_PARKINGS_RES|" + serializeParkings(DbRepository.getAllParkings()));
                        break;
                    case "GET_PARKING_BY_ID":
                        DbQueries.ParkingInfo pi = DbRepository.getParkingById(Integer.parseInt(parts[1]));
                        out.println("PARKING_BY_ID_RES|" + (pi == null ? "NULL" : serializeParking(pi)));
                        break;
                }
            } catch (Exception e) {
                out.println("ERROR|" + e.getMessage());
            }
        }

        private String serializeParkings(List<DbQueries.ParkingInfo> list) {
            List<String> s = new ArrayList<>();
            for (DbQueries.ParkingInfo p : list) {
                s.add(serializeParking(p));
            }
            return String.join(";", s);
        }

        private String serializeParking(DbQueries.ParkingInfo p) {
            // id,name,zoneName,status,vNo,vType,inTime
            return String.format("%d,%s,%s,%s,%s,%s,%s",
                p.id, p.name, p.zoneName, p.status,
                p.vehicleNo == null ? "NULL" : p.vehicleNo,
                p.vehicleType == null ? "NULL" : p.vehicleType,
                p.inTime == null ? "NULL" : p.inTime.toString()
            );
        }
    }
}
