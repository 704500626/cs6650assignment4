package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBSeeder {
//    private static final String URL = "jdbc:mysql://localhost:3306/UPIC";
    private static final String URL = "jdbc:mysql://database-2.cluster-ctou2s6cq6li.us-west-2.rds.amazonaws.com:3306/UPIC";
    private static final String USER = "admin";
    private static final String PASSWORD = "adminadmin";

    public static void main(String[] args) {
        String url = URL;
        String user = USER;
        String password = PASSWORD;
        if (args.length != 0) {
            url = args[0];
            user = args[1];
            password = args[2];
        }
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            insertResorts(conn);
            insertSkiers(conn);
            insertLifts(conn);
            insertSeasons(conn);
            conn.commit();
            System.out.println("Database seeding completed successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertResorts(Connection conn) throws SQLException {
        String sql = "INSERT INTO Resorts (resort_id, resort_name) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 10; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "Resort-" + i);
                stmt.addBatch();
            }
            stmt.executeBatch();
            System.out.println("Inserted Resorts.");
        }
    }

    private static void insertSkiers(Connection conn) throws SQLException {
        String sql = "INSERT INTO Skiers (skier_id) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 100_000; i++) {
                stmt.setInt(1, i);
                stmt.addBatch();
                if (i % 1000 == 0)
                    stmt.executeBatch(); // batch every 1000 for performance
            }
            stmt.executeBatch();
            System.out.println("Inserted Skiers.");
        }
    }

    private static void insertLifts(Connection conn) throws SQLException {
        String sql = "INSERT INTO Lifts (resort_id, lift_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int resortId = 1; resortId <= 10; resortId++) {
                for (int liftId = 1; liftId <= 40; liftId++) {
                    stmt.setInt(1, resortId);
                    stmt.setInt(2, liftId);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
            System.out.println("Inserted Lifts.");
        }
    }

    private static void insertSeasons(Connection conn) throws SQLException {
        String sql = "INSERT INTO Seasons (resort_id, season_id) VALUES (?, ?)";
        List<String> seasons = new ArrayList<>();
        for (int i = 1000; i <= 9999; i++) {
            seasons.add(Integer.toString(i));
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int resortId = 1; resortId <= 10; resortId++) {
                for (String season : seasons) {
                    stmt.setInt(1, resortId);
                    stmt.setString(2, season);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
            System.out.println("Inserted Seasons.");
        }
    }
}
