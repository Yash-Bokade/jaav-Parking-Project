package DB;

import Networking.ParkingClient;
import java.util.List;

public class DbQueries {

    private static boolean useClient() {
        try {
            Class<?> mainClass = Class.forName("Main");
            java.lang.reflect.Method isClientMethod = mainClass.getMethod("isClient");
            return (boolean) isClientMethod.invoke(null);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateUser(String username, String password) {
        if (useClient()) return ParkingClient.getInstance().login(username, password);
        return DbRepository.validateUser(username, password);
    }

    public static List<ParkingInfo> getAllParkings() {
        if (useClient()) return ParkingClient.getInstance().getAllParkings();
        return DbRepository.getAllParkings();
    }

    public static ParkingInfo getParkingById(int id) {
        if (useClient()) return ParkingClient.getInstance().getParkingById(id);
        return DbRepository.getParkingById(id);
    }

    public static boolean updateParkingStatus(int parkingId, String status) {
        // This is usually called via reserve/release now, but keep for compatibility
        if (useClient()) return false; // Not implemented directly via client for safety
        return DbRepository.updateParkingStatus(parkingId, status);
    }

    public static boolean reserveParking(int parkingId, String vehicleNo, String vehicleType) {
        if (useClient()) return ParkingClient.getInstance().reserve(parkingId, vehicleNo, vehicleType);
        return DbRepository.reserveParking(parkingId, vehicleNo, vehicleType);
    }

    public static ParkingInfo releaseParking(String vehicleNo) {
        if (useClient()) return ParkingClient.getInstance().release(vehicleNo);
        return DbRepository.releaseParking(vehicleNo);
    }

    public static List<String> getAllZones() {
        if (useClient()) return ParkingClient.getInstance().getZones();
        return DbRepository.getAllZones();
    }

    public static List<ParkingInfo> getParkingsByZone(String zoneName) {
        if (useClient()) return ParkingClient.getInstance().getParkingsByZone(zoneName);
        return DbRepository.getParkingsByZone(zoneName);
    }

    public static boolean createZone(String zoneName, int spotCount) {
        if (useClient()) return ParkingClient.getInstance().createZone(zoneName, spotCount);
        return DbRepository.createZone(zoneName, spotCount);
    }

    public static boolean updateDbSettings(String url, String port, String user, String pass) {
        return DataRepository.updateDbSettings(url, port, user, pass);
    }

    public static class ParkingInfo {
        public int id;
        public String name;
        public String zoneName;
        public String status;
        public String vehicleNo;
        public String vehicleType;
        public java.time.LocalDateTime inTime;

        public ParkingInfo(int id, String name, String zoneName, String status) {
            this.id = id;
            this.name = name;
            this.zoneName = zoneName;
            this.status = status;
        }

        public ParkingInfo(int id, String name, String zoneName, String status, String vehicleNo, String vehicleType, java.time.LocalDateTime inTime) {
            this.id = id;
            this.name = name;
            this.zoneName = zoneName;
            this.status = status;
            this.vehicleNo = vehicleNo;
            this.vehicleType = vehicleType;
            this.inTime = inTime;
        }
    }
}
