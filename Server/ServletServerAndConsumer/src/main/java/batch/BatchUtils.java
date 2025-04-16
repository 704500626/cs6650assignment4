package batch;

import model.Configuration;

import java.sql.*;

public class BatchUtils {
    public static String getTempKey(String key) {
        return "temp:" + key;
    }

    public static long estimateRowCount(Configuration config) {
        try (Connection conn = DriverManager.getConnection(config.MYSQL_READ_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_FULL_ROW_COUNT_SQL);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("row_count");
            }
        } catch (SQLException e) {
            System.err.println("[estimateRowCount] Failed to count rows: " + e.getMessage());
        }
        return Long.MAX_VALUE; // fallback: don't run full aggregation
    }
}
