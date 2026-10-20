Hello 
so this is my Java Mini Project ,

# Smart City Parking application
| PRN        | NAME                 |
|------------|----------------------|
| 1012612004 | Shardul Patil        |
| 1012612010 | Heramb Dharmadhakari |
| 1012612012 | Yashraj Shinde       |
| 1012612016 | Yash Boakde          |
---

> This Application includes 
> 1. GUI.GUI (Swing with MDI)
> 2. In-memory Data (previously MySql JDBC)
> 3. Threads
> 4. Sockets for Multi-device connectivity

## Data Setup

The application currently uses in-memory data (LinkedList/ArrayList/HashMap) for demonstration purposes. To switch back to Database, modify `DbQueries.java` to use `DbConnector`.

```sql
CREATE DATABASE parking_db;
USE parking_db;

CREATE TABLE User (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(15),
    address TEXT,
    role VARCHAR(20)
);

-- Note: Each zone now has its own table named 'ParkingZone_<ZoneName>'
-- Example for 'Zone A':
CREATE TABLE ParkingZone_ZoneA (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20),
    status VARCHAR(20) DEFAULT 'available',
    vehicle_no VARCHAR(20),
    vehicle_type VARCHAR(20),
    in_time DATETIME
);

-- Example for 'Zone B':
CREATE TABLE ParkingZone_ZoneB (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20),
    status VARCHAR(20) DEFAULT 'available',
    vehicle_no VARCHAR(20),
    vehicle_type VARCHAR(20),
    in_time DATETIME
);

-- Table to track all available zones
CREATE TABLE Zones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    zone_name VARCHAR(50) UNIQUE NOT NULL,
    spot_count INT NOT NULL
);

-- Sample Data
INSERT INTO User (name, password, role) VALUES ('admin', 'admin123', 'admin');
INSERT INTO Zones (zone_name, spot_count) VALUES ('Zone A', 10), ('Zone B', 8);
```

## Structure
    
Project
```
 |-src/
 |  |-Main.java
 |  |-GUI/
 |  |  |-GUI.java
 |  |  |-NorthPanel.java
 |  |  |-InternalParkingFrame.java
 |  |-Networking/
 |  |  |-ParkingServer.java
 |  |-DB/
 |  |  |-DbConnector.java
 |  |  |-DbQueries.java
 |  |  |-DataRepository.java
 ```
 ---
 `Implemented with Swing MDI, In-memory Data, Sockets, Threads, Reservation/Exit Forms, and Printable Receipts`
 ---  