package DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class DbRepository {

    public static void initDatabase() {
        try (Connection conn = DbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Re-create the database if it's simpler or just the tables
            // But usually we assume the DB exists as per Readme.Md
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS User (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(50) NOT NULL," +
                    "password VARCHAR(50) NOT NULL," +
                    "role VARCHAR(20))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Zones (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "zone_name VARCHAR(50) UNIQUE NOT NULL," +
                    "spot_count INT NOT NULL)");

            // Check if admin exists
            ResultSet rs = stmt.executeQuery("SELECT * FROM User WHERE name='admin'");
            if (!rs.next()) {
                stmt.executeUpdate("INSERT INTO User (name, password, role) VALUES ('admin', 'admin123', 'admin')");
            }
        } catch (SQLException e) {
            System.err.println("[DEBUG_LOG] DB Init Error: " + e.getMessage());
        }
    }

    public static List<String> getAllZones() {
        List<String> zones = new ArrayList<>();
        try (Connection conn = DbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT zone_name FROM Zones")) {
            while (rs.next()) {
                zones.add(rs.getString("zone_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return zones;
    }

    public static List<DbQueries.ParkingInfo> getParkingsByZone(String zoneName) {
        List<DbQueries.ParkingInfo> parkings = new ArrayList<>();
        String tableName = "ParkingZone_" + zoneName.replace(" ", "");
        try (Connection conn = DbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("in_time");
                LocalDateTime inTime = (ts != null) ? ts.toLocalDateTime() : null;
                parkings.add(new DbQueries.ParkingInfo(
                        rs.getInt("id"),
                        rs.getString("name"),
                        zoneName,
                        rs.getString("status"),
                        rs.getString("vehicle_no"),
                        rs.getString("vehicle_type"),
                        inTime
                ));
            }
        } catch (SQLException e) {
            // Table might not exist yet
            System.err.println("[DEBUG_LOG] Error fetching from " + tableName + ": " + e.getMessage());
        }
        return parkings;
    }

    public static boolean createZone(String zoneName, int spotCount) {
        String tableName = "ParkingZone_" + zoneName.replace(" ", "");
        try (Connection conn = DbConnector.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Register zone
                PreparedStatement psZone = conn.prepareStatement("INSERT INTO Zones (zone_name, spot_count) VALUES (?, ?)");
                psZone.setString(1, zoneName);
                psZone.setInt(2, spotCount);
                psZone.executeUpdate();

                // Create table
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE " + tableName + " (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "name VARCHAR(20)," +
                        "status VARCHAR(20) DEFAULT 'available'," +
                        "vehicle_no VARCHAR(20)," +
                        "vehicle_type VARCHAR(20)," +
                        "in_time DATETIME)");

                // Add spots
                PreparedStatement psSpot = conn.prepareStatement("INSERT INTO " + tableName + " (name) VALUES (?)");
                for (int i = 1; i <= spotCount; i++) {
                    psSpot.setString(1, String.valueOf(i));
                    psSpot.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateUser(String username, String password) {
        try (Connection conn = DbConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE name=? AND password=?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<DbQueries.ParkingInfo> getAllParkings() {
        List<DbQueries.ParkingInfo> all = new ArrayList<>();
        List<String> zones = getAllZones();
        for (String zone : zones) {
            all.addAll(getParkingsByZone(zone));
        }
        return all;
    }

    public static DbQueries.ParkingInfo getParkingById(int id) {
        // This is tricky because id might not be unique across tables IF they use AUTO_INCREMENT per table.
        // However, the original app seems to expect a global ID search.
        // If we follow the current design where each table has its own IDs, we'd need to search all tables.
        List<String> zones = getAllZones();
        for (String zone : zones) {
            String tableName = "ParkingZone_" + zone.replace(" ", "");
            try (Connection conn = DbConnector.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE id=?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("in_time");
                    LocalDateTime inTime = (ts != null) ? ts.toLocalDateTime() : null;
                    return new DbQueries.ParkingInfo(
                            rs.getInt("id"),
                            rs.getString("name"),
                            zone,
                            rs.getString("status"),
                            rs.getString("vehicle_no"),
                            rs.getString("vehicle_type"),
                            inTime
                    );
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }

    public static boolean reserveParking(int parkingId, String vehicleNo, String vehicleType) {
        // To reserve, we need to know which zone/table. 
        // If we only have ID, we have to find it first or iterate.
        List<String> zones = getAllZones();
        for (String zone : zones) {
            String tableName = "ParkingZone_" + zone.replace(" ", "");
            try (Connection conn = DbConnector.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE " + tableName + " SET status='reserved', vehicle_no=?, vehicle_type=?, in_time=? " +
                         "WHERE id=? AND status='available'")) {
                ps.setString(1, vehicleNo);
                ps.setString(2, vehicleType);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(4, parkingId);
                int updated = ps.executeUpdate();
                if (updated > 0) return true;
            } catch (SQLException ignored) {}
        }
        return false;
    }

    public static DbQueries.ParkingInfo releaseParking(String vehicleNo) {
        List<String> zones = getAllZones();
        for (String zone : zones) {
            String tableName = "ParkingZone_" + zone.replace(" ", "");
            try (Connection conn = DbConnector.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    DbQueries.ParkingInfo info = null;
                    PreparedStatement psSel = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE vehicle_no=? AND status='reserved'");
                    psSel.setString(1, vehicleNo);
                    ResultSet rs = psSel.executeQuery();
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp("in_time");
                        LocalDateTime inTime = (ts != null) ? ts.toLocalDateTime() : null;
                        info = new DbQueries.ParkingInfo(
                                rs.getInt("id"),
                                rs.getString("name"),
                                zone,
                                rs.getString("status"),
                                rs.getString("vehicle_no"),
                                rs.getString("vehicle_type"),
                                inTime
                        );

                        PreparedStatement psUpd = conn.prepareStatement(
                                "UPDATE " + tableName + " SET status='available', vehicle_no=NULL, vehicle_type=NULL, in_time=NULL " +
                                "WHERE vehicle_no=?");
                        psUpd.setString(1, vehicleNo);
                        psUpd.executeUpdate();
                        conn.commit();
                        return info;
                    }
                    conn.rollback();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }
    
    public static boolean updateParkingStatus(int parkingId, String status) {
        // Implementation for general status update
        List<String> zones = getAllZones();
        for (String zone : zones) {
            String tableName = "ParkingZone_" + zone.replace(" ", "");
            try (Connection conn = DbConnector.getConnection()) {
                String sql;
                if ("available".equalsIgnoreCase(status)) {
                    sql = "UPDATE " + tableName + " SET status=?, vehicle_no=NULL, vehicle_type=NULL, in_time=NULL WHERE id=?";
                } else {
                    sql = "UPDATE " + tableName + " SET status=? WHERE id=?";
                }
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, status);
                ps.setInt(2, parkingId);
                if (ps.executeUpdate() > 0) return true;
            } catch (SQLException ignored) {}
        }
        return false;
    }
}
