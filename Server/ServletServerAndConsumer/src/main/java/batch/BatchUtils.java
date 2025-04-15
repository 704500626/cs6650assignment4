package batch;

import model.Configuration;

import java.sql.*;

public class BatchUtils {
    public static String getTempKey(String key) {
        return "temp:" + key;
    }

    public static long estimateRowCount(Configuration config) {
        try (Connection conn = DriverManager.getConnection(config.MYSQL_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD); PreparedStatement stmt = conn.prepareStatement(config.AGGREGATION_FULL_ROW_COUNT_SQL)) {
            stmt.setString(1, config.MYSQL_TABLE_SCHEMA);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("TABLE_ROWS");
                }
            }
        } catch (SQLException e) {
            System.err.println("[estimateRowCount] Failed to query TABLE_ROWS: " + e.getMessage());
        }
        return Long.MAX_VALUE; // fallback: don't run full aggregation
    }
}
