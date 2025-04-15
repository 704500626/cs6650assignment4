package dao;

import com.rabbitmq.client.Channel;
import model.Configuration;
import model.LiftRideEventMsg;
import utils.DBUtils;

import java.sql.*;
import java.util.*;

public class LiftRideWriter {
    private final Configuration config;
    private final Connection conn;
    private final PreparedStatement stmt;
    // Buffer of events and their associated RabbitMQ delivery tags
    private final List<LiftRideEventMsg> buffer = new ArrayList<>();
    private final List<Long> deliveryTags = new ArrayList<>();
    private final Channel channel; // Associated RabbitMQ channel, per channel per DB writer pattern

    public LiftRideWriter(Configuration config, Channel channel) throws SQLException {
        this.config = config;
        this.channel = channel;
        this.conn = DriverManager.getConnection(config.MYSQL_WRITE_URL, config.MYSQL_USERNAME, config.MYSQL_PASSWORD);
        this.conn.setAutoCommit(false);
        this.stmt = conn.prepareStatement(config.MYSQL_INSERT_SQL);
    }

    /**
     * Adds an event along with its delivery tag to the in‐memory buffer.
     * When the buffer reaches the configured batch size, flush() is called.
     */
    public synchronized void addEvent(LiftRideEventMsg event, long deliveryTag) throws SQLException {
        buffer.add(event);
        deliveryTags.add(deliveryTag);
        if (buffer.size() >= config.MYSQL_WRITE_BATCH_SIZE) {
            flush();
        }
    }

    /**
     * Flushes the current batch to the DB.
     * On a successful batch execution, it uses a multiple ACK to acknowledge all messages at once.
     * On partial failures, it commits the successful portion and retries the failed ones individually.
     */
    public synchronized void flush() throws SQLException {
        if (buffer.isEmpty()) return;
        // Create working copies of the events and delivery tags.
        List<LiftRideEventMsg> eventsToInsert = new ArrayList<>(buffer);
        List<Long> tagsToAck = new ArrayList<>(deliveryTags);
        buffer.clear();
        deliveryTags.clear();

        int attempt = 0;
        while (!eventsToInsert.isEmpty() && attempt < config.MAX_RETRIES) {
            try {
                stmt.clearBatch();
                for (LiftRideEventMsg event : eventsToInsert) {
                    DBUtils.setParametersForEvent(stmt, event);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit(); // Commit successful commands.
                ackWithRetry(tagsToAck.get(tagsToAck.size() - 1), true);
                return;
            } catch (BatchUpdateException bue) {
                conn.commit(); // Commit partial successful commands despite the exception
                int[] counts = bue.getUpdateCounts();
                List<LiftRideEventMsg> retryBatch = new ArrayList<>();
                List<Long> retryTags = new ArrayList<>();
                if (counts.length == eventsToInsert.size()) { // In this case the DB continues after the failure
                    int lastSuccess = -1;
                    for (int i = 0; i < counts.length; i++) {
                        if (counts[i] == Statement.EXECUTE_FAILED) {
                            boolean permanentlyFailed = !insertIndividuallyWithRetry(eventsToInsert.get(i));
                            if (permanentlyFailed) nackWithRetry(tagsToAck.get(i));
                            else ackWithRetry(tagsToAck.get(i), true);
                        } else {
                            lastSuccess = i;
                        }
                    }
                    if (lastSuccess != -1) ackWithRetry(tagsToAck.get(lastSuccess), true);
                } else { // In this case the DB stops at the first failure
                    int firstFailedIndex = counts.length;
                    boolean permanentlyFailed = !insertIndividuallyWithRetry(eventsToInsert.get(firstFailedIndex));
                    if (permanentlyFailed) nackWithRetry(tagsToAck.get(firstFailedIndex));
                    else ackWithRetry(tagsToAck.get(firstFailedIndex), true);
                    if (firstFailedIndex > 0) ackWithRetry(tagsToAck.get(firstFailedIndex - 1), true);
                    retryBatch.addAll(eventsToInsert.subList(firstFailedIndex + 1, eventsToInsert.size()));
                    retryTags.addAll(tagsToAck.subList(firstFailedIndex + 1, eventsToInsert.size()));
                }
                eventsToInsert = retryBatch;
                tagsToAck = retryTags;
                sleep(++attempt);
            } catch (SQLException e) {
                conn.rollback();
                sleep(++attempt);
            }
        }

        // Final individual insertion for remaining failed events
        for (int i = 0; i < eventsToInsert.size(); i++) {
            LiftRideEventMsg event = eventsToInsert.get(i);
            long tag = tagsToAck.get(i);
            if (insertIndividuallyWithRetry(event)) ackWithRetry(tag, false);
            else nackWithRetry(tag);
        }
    }

    private boolean insertIndividuallyWithRetry(LiftRideEventMsg event) {
        int attempt = 0;
        while (attempt < config.MAX_RETRIES) {
            try {
                stmt.clearParameters();
                DBUtils.setParametersForEvent(stmt, event);
                stmt.executeUpdate();
                conn.commit();
                return true;
            } catch (SQLException e) {
                String sqlState = e.getSQLState();
                if (sqlState != null && sqlState.startsWith("23")) {
                    return false; // constraint violation
                }
                attempt++;
                sleep(attempt);
            }
        }
        return false;
    }

    private void ackWithRetry(long tag, boolean multiple) {
        int attempts = 0;
        while (attempts < config.MAX_RETRIES) {
            try {
                channel.basicAck(tag, multiple);
                return;
            } catch (Exception e) {
                attempts++;
                sleep(attempts);
            }
        }
    }

    private void nackWithRetry(long tag) {
        int attempts = 0;
        while (attempts < config.MAX_RETRIES) {
            try {
                channel.basicNack(tag, false, false);
                return;
            } catch (Exception e) {
                attempts++;
                sleep(attempts);
            }
        }
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void close() {
        try {
            flush();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

