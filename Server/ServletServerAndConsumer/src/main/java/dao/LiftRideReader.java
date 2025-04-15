package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import grpc.LiftRideReadProto.*;
import model.Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LiftRideReader {
    private final Configuration config;
    private final HikariDataSource dataSource;

    public LiftRideReader(Configuration config) {
        this.config = config;
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.MYSQL_READ_URL);
        hikariConfig.setUsername(config.MYSQL_USERNAME);
        hikariConfig.setPassword(config.MYSQL_PASSWORD);
        hikariConfig.setMaximumPoolSize(config.MYSQL_READ_MAX_POOL_SIZE);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaxLifetime(600000);
        hikariConfig.setConnectionTimeout(3000);
        hikariConfig.setPoolName("LiftRideReadPool");
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public int getResortUniqueSkiers(int resortId, String seasonId, int dayId) throws SQLException {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(config.MYSQL_GET_UNIQUE_SKIERS_SQL)) {
            stmt.setInt(1, resortId);
            stmt.setString(2, seasonId);
            stmt.setInt(3, dayId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int getSkierDayVertical(int resortId, String seasonId, int dayId, int skierId) throws SQLException {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(config.MYSQL_GET_DAILY_VERTICAL_SQL)) {
            stmt.setInt(1, skierId);
            stmt.setInt(2, resortId);
            stmt.setString(3, seasonId);
            stmt.setInt(4, dayId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<VerticalRecord> getSkierResortTotals(int skierId, int resortId, String seasonId) throws SQLException {
        String sql;
        boolean withSeason = seasonId != null && !seasonId.isEmpty();
        if (withSeason) {
            sql = config.MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1;
        } else {
            sql = config.MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2;
        }

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, skierId);
            stmt.setInt(2, resortId);
            if (withSeason) stmt.setString(3, seasonId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<VerticalRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(VerticalRecord.newBuilder().setSeasonID(rs.getString(1)).setTotalVertical(rs.getInt(2)).build());
                }
                return result;
            }
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}