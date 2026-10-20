package DB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRepository {
    // Each zone now mapped to its own list of parkings to simulate table per zone
    private static Map<String, List<DbQueries.ParkingInfo>> zoneTables = new HashMap<>();
    private static List<String> zones = new ArrayList<>();
    private static Map<String, String> users = new HashMap<>();
    private static int nextId = 1;

    static {
        // Initialize with sample data
        users.put("admin", "admin123");
        
        createZone("Zone A", 10);
        createZone("Zone B", 8);
    }

    public static synchronized List<String> getAllZones() {
        return new ArrayList<>(zones);
    }

    public static synchronized List<DbQueries.ParkingInfo> getParkingsByZone(String zoneName) {
        return new ArrayList<>(zoneTables.getOrDefault(zoneName, new ArrayList<>()));
    }

    public static synchronized boolean createZone(String zoneName, int spotCount) {
        if (zones.contains(zoneName)) return false;
        zones.add(zoneName);
        List<DbQueries.ParkingInfo> zoneParkings = new ArrayList<>();
        for (int i = 1; i <= spotCount; i++) {
            zoneParkings.add(new DbQueries.ParkingInfo(nextId++, String.valueOf(i), zoneName, "available"));
        }
        zoneTables.put(zoneName, zoneParkings);
        return true;
    }

    public static synchronized boolean validateUser(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    public static synchronized List<DbQueries.ParkingInfo> getAllParkings() {
        List<DbQueries.ParkingInfo> all = new ArrayList<>();
        for (List<DbQueries.ParkingInfo> list : zoneTables.values()) {
            all.addAll(list);
        }
        return all;
    }

    public static synchronized DbQueries.ParkingInfo getParkingById(int id) {
        for (List<DbQueries.ParkingInfo> list : zoneTables.values()) {
            for (DbQueries.ParkingInfo p : list) {
                if (p.id == id) return p;
            }
        }
        return null;
    }

    public static synchronized boolean updateParkingStatus(int parkingId, String status) {
        for (List<DbQueries.ParkingInfo> list : zoneTables.values()) {
            for (DbQueries.ParkingInfo p : list) {
                if (p.id == parkingId) {
                    p.status = status;
                    if ("available".equalsIgnoreCase(status)) {
                        p.vehicleNo = null;
                        p.vehicleType = null;
                        p.inTime = null;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static synchronized boolean reserveParking(int parkingId, String vehicleNo, String vehicleType) {
        for (List<DbQueries.ParkingInfo> list : zoneTables.values()) {
            for (DbQueries.ParkingInfo p : list) {
                if (p.id == parkingId && "available".equalsIgnoreCase(p.status)) {
                    p.status = "reserved";
                    p.vehicleNo = vehicleNo;
                    p.vehicleType = vehicleType;
                    p.inTime = java.time.LocalDateTime.now();
                    return true;
                }
            }
        }
        return false;
    }

    public static synchronized DbQueries.ParkingInfo releaseParking(String vehicleNo) {
        for (List<DbQueries.ParkingInfo> list : zoneTables.values()) {
            for (DbQueries.ParkingInfo p : list) {
                if (vehicleNo.equalsIgnoreCase(p.vehicleNo) && "reserved".equalsIgnoreCase(p.status)) {
                    DbQueries.ParkingInfo info = new DbQueries.ParkingInfo(p.id, p.name, p.zoneName, p.status, p.vehicleNo, p.vehicleType, p.inTime);
                    p.status = "available";
                    p.vehicleNo = null;
                    p.vehicleType = null;
                    p.inTime = null;
                    return info;
                }
            }
        }
        return null;
    }

    public static synchronized boolean updateDbSettings(String url, String port, String user, String pass) {
        System.out.println("Updating DB Settings: " + url + ":" + port + " User: " + user);
        DbConnector.setConfig(url, port, user, pass);
        return true;
    }
}
