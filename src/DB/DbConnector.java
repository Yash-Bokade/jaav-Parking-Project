package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnector {
    private static String dbUrl = "localhost";
    private static String dbPort = "3306";
    private static String dbUser = "root";
    private static String dbPass = "";

    public static void setConfig(String url, String port, String user, String pass) {
        dbUrl = url;
        dbPort = port;
        dbUser = user;
        dbPass = pass;
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DEBUG_LOG] MySQL Driver not found: " + e.getMessage());
        }
        String fullUrl = "jdbc:mysql://" + dbUrl + ":" + dbPort + "/parking_db";
        return DriverManager.getConnection(fullUrl, dbUser, dbPass);
    }
}
