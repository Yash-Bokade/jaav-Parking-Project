package Networking;

import DB.DbQueries;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingClient {
    private static ParkingClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIp;
    private int serverPort;
    private Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private List<Runnable> refreshCallbacks = new ArrayList<>();
    private List<Runnable> loadZonesCallbacks = new ArrayList<>();

    private ParkingClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public static synchronized ParkingClient getInstance(String ip, int port) {
        if (instance == null) {
            instance = new ParkingClient(ip, port);
            instance.connect();
        }
        return instance;
    }

    public static ParkingClient getInstance() {
        return instance;
    }

    private void connect() {
        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::listenToServer).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("REFRESH")) {
                    for (Runnable r : refreshCallbacks) r.run();
                } else if (line.equals("LOAD_ZONES")) {
                    for (Runnable r : loadZonesCallbacks) r.run();
                } else {
                    String[] parts = line.split("\\|", 2);
                    String resType = parts[0];
                    CompletableFuture<String> future = pendingRequests.remove(resType);
                    if (future != null) {
                        future.complete(parts.length > 1 ? parts[1] : "");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server.");
        }
    }

    private String sendRequest(String cmd, String resType) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(resType, future);
        out.println(cmd);
        try {
            return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingRequests.remove(resType);
            return null;
        }
    }

    public void onRefresh(Runnable r) { refreshCallbacks.add(r); }
    public void onLoadZones(Runnable r) { loadZonesCallbacks.add(r); }

    public boolean login(String user, String pass) {
        String res = sendRequest("LOGIN|" + user + "|" + pass, "LOGIN_RES");
        return Boolean.parseBoolean(res);
    }

    public List<String> getZones() {
        String res = sendRequest("GET_ZONES", "ZONES_RES");
        if (res == null || res.isEmpty()) return Collections.emptyList();
        return Arrays.asList(res.split(","));
    }

    public List<DbQueries.ParkingInfo> getParkingsByZone(String zoneName) {
        String res = sendRequest("GET_PARKINGS_ZONE|" + zoneName, "PARKINGS_RES");
        return deserializeParkings(res);
    }

    public boolean reserve(int id, String vNo, String type) {
        String res = sendRequest("RESERVE|" + id + "|" + vNo + "|" + type, "RESERVE_RES");
        return Boolean.parseBoolean(res);
    }

    public DbQueries.ParkingInfo release(String vNo) {
        String res = sendRequest("RELEASE|" + vNo, "RELEASE_RES");
        if (res != null && res.startsWith("SUCCESS|")) {
            return deserializeParking(res.substring(8));
        }
        return null;
    }

    public boolean createZone(String name, int count) {
        String res = sendRequest("CREATE_ZONE|" + name + "|" + count, "CREATE_ZONE_RES");
        return Boolean.parseBoolean(res);
    }

    public List<DbQueries.ParkingInfo> getAllParkings() {
        String res = sendRequest("GET_ALL_PARKINGS", "ALL_PARKINGS_RES");
        return deserializeParkings(res);
    }

    public DbQueries.ParkingInfo getParkingById(int id) {
        String res = sendRequest("GET_PARKING_BY_ID|" + id, "PARKING_BY_ID_RES");
        if (res == null || res.equals("NULL")) return null;
        return deserializeParking(res);
    }

    private List<DbQueries.ParkingInfo> deserializeParkings(String data) {
        if (data == null || data.isEmpty()) return Collections.emptyList();
        List<DbQueries.ParkingInfo> list = new ArrayList<>();
        String[] items = data.split(";");
        for (String item : items) {
            list.add(deserializeParking(item));
        }
        return list;
    }

    private DbQueries.ParkingInfo deserializeParking(String item) {
        String[] p = item.split(",");
        int id = Integer.parseInt(p[0]);
        String name = p[1];
        String zoneName = p[2];
        String status = p[3];
        String vNo = p[4].equals("NULL") ? null : p[4];
        String vType = p[5].equals("NULL") ? null : p[5];
        LocalDateTime inTime = p[6].equals("NULL") ? null : LocalDateTime.parse(p[6]);
        return new DbQueries.ParkingInfo(id, name, zoneName, status, vNo, vType, inTime);
    }
}
