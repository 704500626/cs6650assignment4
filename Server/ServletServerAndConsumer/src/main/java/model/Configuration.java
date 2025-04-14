package model;

import java.util.Properties;

public class Configuration {
    // RabbitMQ Configuration
    public String RABBITMQ_HOST = "localhost"; // RabbitMQ broker IP
    public String RABBITMQ_USERNAME = "admin"; // username
    public String RABBITMQ_PASSWORD = "admin"; // password
    public String EXCHANGE_NAME = "skiers_exchange"; // Exchange name for the messages
    public String ROUTING_KEY = "skiers.route"; // Routing key for the messages
    public String QUEUE_NAME = "skiers_queue"; // Queue name for the messages

    public String MYSQL_URL = "jdbc:mysql://localhost:3306/UPIC";
    public String MYSQL_USERNAME = "admin";
    public String MYSQL_PASSWORD = "admin";
    public String MYSQL_INSERT_SQL = "INSERT INTO LiftRides (skier_id, resort_id, season_id, day_id, lift_id, ride_time) VALUES (?, ?, ?, ?, ?, ?)";
    public String MYSQL_GET_UNIQUE_SKIERS_SQL = "SELECT COUNT(DISTINCT skier_id) AS unique_skiers FROM LiftRides WHERE resort_id = ? AND season_id = ? AND day_id = ?";
    public String MYSQL_GET_DAILY_VERTICAL_SQL = "SELECT SUM(10 * lift_id) AS total_vertical FROM LiftRides WHERE skier_id = ? AND resort_id = ? AND season_id = ? AND day_id = ?";
    public String MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1 = "SELECT season_id, SUM(lift_id * 10) AS total_vertical FROM LiftRides WHERE skier_id = ? AND resort_id = ? AND season_id = ? GROUP BY season_id";
    public String MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2 = "SELECT season_id, SUM(lift_id * 10) AS total_vertical FROM LiftRides WHERE skier_id = ? AND resort_id = ? GROUP BY season_id";
    public int MYSQL_MAX_POOL_SIZE = 16;
    public int MYSQL_BATCH_SIZE = 100;
    public int MYSQL_FLUSH_INTERVAL_MS = 500;

    public int EVENT_QUEUE_SIZE = 5000;
    public int WORKER_POOL_SIZE = 20;

    public int MAX_QUEUED_MSG = 1000;
    public int CIRCUIT_BREAKER_THRESHOLD = 10000;
    public int CIRCUIT_BREAKER_TIMEOUT_MS = 2000;
    public int QUEUE_MONITOR_THREAD_COUNT = 10;
    public int QUEUE_MONITOR_INTERVAL_MS = 100;
    public int MAX_BACKOFF_MS = 2000;

    public int MAX_TOKENS = 100;
    public int REFILL_RATE_PER_SECOND = 1600;

    public int NUM_QUEUES = 2; // Number of queues
    public int NUM_CONNECTIONS = 2; // Number of connections
    public int NUM_CHANNELS_PER_CONNECTION = 50; // Channels per connection

    // Channel pool configuration
    public int THREAD_POOL_SIZE = 500; // Thread pool size
    public int MAX_CHANNELS = 500; // Maximum number of channels
    public int CHANNEL_POOL_SIZE = 50; // Channel pool size
    public int MIN_CONSUMERS = 100;   // Minimum number of consumers
    public int MAX_CONSUMERS = 1000;  // Maximum number of consumers
    public int MAX_RETRIES = 5; // Maximum number of retries
    public int INITIAL_RETRY_DELAY_MS = 200; // Initial delay before retrying
    public int REQUEST_HEART_BEAT = 30; // Request heart beat in seconds
    public int PREFETCH_COUNT = 10; // Prefect message count
    public int RABBITMQ_BATCH_SIZE = 20;  // Number of messages per batch
    public int RABBITMQ_FLUSH_INTERVAL_MS = 500;  // Flush acknowledgments every 500ms
    public int NUM_CHANNELS_PER_QUEUE = 5; // Number of channels created for each queue

    public String REDIS_HOST;
    public int REDIS_PORT;

    public String REDIS_KEY_UNIQUE_SKIER_COUNT;
    public String REDIS_KEY_VERTICAL_WITH_SKIER;
    public String REDIS_KEY_VERTICAL_COUNT;


    public Configuration() {}

    public Configuration(Properties properties) {
        String local = properties.getProperty("local");
        if (local.equals("true")) {
            RABBITMQ_HOST = "localhost";
        } else {
            RABBITMQ_HOST = properties.getProperty("RABBITMQ_HOST");
        }
        RABBITMQ_USERNAME = properties.getProperty("RABBITMQ_USERNAME");
        RABBITMQ_PASSWORD = properties.getProperty("RABBITMQ_PASSWORD");
        EXCHANGE_NAME = properties.getProperty("EXCHANGE_NAME");
        ROUTING_KEY = properties.getProperty("ROUTING_KEY");
        QUEUE_NAME = properties.getProperty("QUEUE_NAME");

        MYSQL_URL = properties.getProperty("MYSQL_URL");
        MYSQL_USERNAME = properties.getProperty("MYSQL_USERNAME");
        MYSQL_PASSWORD = properties.getProperty("MYSQL_PASSWORD");
        MYSQL_INSERT_SQL = properties.getProperty("MYSQL_INSERT_SQL");
        MYSQL_GET_UNIQUE_SKIERS_SQL = properties.getProperty("MYSQL_GET_UNIQUE_SKIERS_SQL");
        MYSQL_GET_DAILY_VERTICAL_SQL = properties.getProperty("MYSQL_GET_DAILY_VERTICAL_SQL");
        MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1 = properties.getProperty("MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1");
        MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2 = properties.getProperty("MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2");

        MYSQL_MAX_POOL_SIZE = Integer.parseInt(properties.getProperty("MYSQL_MAX_POOL_SIZE"));
        MYSQL_BATCH_SIZE = Integer.parseInt(properties.getProperty("MYSQL_BATCH_SIZE"));
        MYSQL_FLUSH_INTERVAL_MS = Integer.parseInt(properties.getProperty("MYSQL_FLUSH_INTERVAL_MS"));

        EVENT_QUEUE_SIZE = Integer.parseInt(properties.getProperty("EVENT_QUEUE_SIZE"));
        WORKER_POOL_SIZE = Integer.parseInt(properties.getProperty("WORKER_POOL_SIZE"));

        MAX_QUEUED_MSG = Integer.parseInt(properties.getProperty("MAX_QUEUED_MSG"));
        CIRCUIT_BREAKER_THRESHOLD = Integer.parseInt(properties.getProperty("CIRCUIT_BREAKER_THRESHOLD"));
        CIRCUIT_BREAKER_TIMEOUT_MS = Integer.parseInt(properties.getProperty("CIRCUIT_BREAKER_TIMEOUT_MS"));
        QUEUE_MONITOR_THREAD_COUNT = Integer.parseInt(properties.getProperty("QUEUE_MONITOR_THREAD_COUNT"));
        QUEUE_MONITOR_INTERVAL_MS = Integer.parseInt(properties.getProperty("QUEUE_MONITOR_INTERVAL_MS"));
        MAX_BACKOFF_MS = Integer.parseInt(properties.getProperty("MAX_BACKOFF_MS"));

        MAX_TOKENS = Integer.parseInt(properties.getProperty("MAX_TOKENS"));
        REFILL_RATE_PER_SECOND = Integer.parseInt(properties.getProperty("REFILL_RATE_PER_SECOND"));

        NUM_QUEUES = Integer.parseInt(properties.getProperty("NUM_QUEUES"));
        NUM_CONNECTIONS = Integer.parseInt(properties.getProperty("NUM_CONNECTIONS"));
        NUM_CHANNELS_PER_CONNECTION = Integer.parseInt(properties.getProperty("NUM_CHANNELS_PER_CONNECTION"));

        THREAD_POOL_SIZE = Integer.parseInt(properties.getProperty("THREAD_POOL_SIZE"));
        MAX_CHANNELS = Integer.parseInt(properties.getProperty("MAX_CHANNELS"));
        CHANNEL_POOL_SIZE = Integer.parseInt(properties.getProperty("CHANNEL_POOL_SIZE"));
        MIN_CONSUMERS = Integer.parseInt(properties.getProperty("MIN_CONSUMERS"));
        MAX_CONSUMERS = Integer.parseInt(properties.getProperty("MAX_CONSUMERS"));
        MAX_RETRIES = Integer.parseInt(properties.getProperty("MAX_RETRIES"));
        INITIAL_RETRY_DELAY_MS = Integer.parseInt(properties.getProperty("INITIAL_RETRY_DELAY_MS"));
        REQUEST_HEART_BEAT = Integer.parseInt(properties.getProperty("REQUEST_HEART_BEAT"));
        PREFETCH_COUNT = Integer.parseInt(properties.getProperty("PREFETCH_COUNT"));
        RABBITMQ_BATCH_SIZE = Integer.parseInt(properties.getProperty("RABBITMQ_BATCH_SIZE"));
        RABBITMQ_FLUSH_INTERVAL_MS = Integer.parseInt(properties.getProperty("RABBITMQ_FLUSH_INTERVAL_MS"));
        NUM_CHANNELS_PER_QUEUE = Integer.parseInt(properties.getProperty("NUM_CHANNELS_PER_QUEUE"));

        REDIS_HOST = properties.getProperty("REDIS_HOST");
        REDIS_PORT = Integer.parseInt(properties.getProperty("REDIS_PORT"));

        REDIS_KEY_UNIQUE_SKIER_COUNT = properties.getProperty("REDIS_KEY_UNIQUE_SKIER_COUNT");
        REDIS_KEY_VERTICAL_WITH_SKIER= properties.getProperty("REDIS_KEY_VERTICAL_WITH_SKIER");
        REDIS_KEY_VERTICAL_COUNT = properties.getProperty("REDIS_KEY_VERTICAL_COUNT");
    }

    public String getREDIS_HOST() {
        return REDIS_HOST;
    }

    public int getREDIS_PORT() {
        return REDIS_PORT;
    }
}
