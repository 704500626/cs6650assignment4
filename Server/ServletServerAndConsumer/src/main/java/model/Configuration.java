package model;

import java.util.Properties;

public class Configuration {
    // RabbitMQ Configuration
    public String RABBITMQ_HOST = "localhost"; // RabbitMQ broker IP
    public String RABBITMQ_USERNAME = "admin"; // username
    public String RABBITMQ_PASSWORD = "admin"; // password
    public String RABBITMQ_EXCHANGE_NAME = "skiers_exchange"; // Exchange name for the messages
    public String RABBITMQ_ROUTING_KEY = "skiers.route"; // Routing key for the messages
    public String RABBITMQ_QUEUE_NAME_PREFIX = "skiers_queue"; // Queue name for the messages
    public int RABBITMQ_MAX_QUEUED_MSG = 5000; // KNOB: the maximal desired number of queued messages in the RMQ(all queues), if there are more messages than this threshold, the request will be asked to wait until the number queued messages fall under the threshold
    public int RABBITMQ_CIRCUIT_BREAKER_THRESHOLD = 10000; // KNOB: the maximal allowed number of queued messages in the RMQ(all queues), if there are more messages than this threshold, circuit breaker will be turn on and new requests will be rejected
    public int RABBITMQ_CIRCUIT_BREAKER_TIMEOUT_MS = 2000; // KNOB(minor): The number of milliseconds the circuit breaker will be on
    public int RABBITMQ_QUEUE_MONITOR_THREAD_COUNT = 5; // KNOB(minor): the number or threads that are used to monitor the total number of queued messages
    public int RABBITMQ_QUEUE_MONITOR_INTERVAL_MS = 100; // KNOB(minor): the time interval of checking the total number of queued messages
    public int RABBITMQ_NUM_QUEUES = 10; // KNOB(major): the number of queues used on RMQ to store and process the messages. Since each queue in RMQ is single-threaded, more queues can improve concurrency. On the production side, messages will be evenly distributed to all N queues. On the consumption side, we assign each queue an equal number of consumers(channels).
    public int RABBITMQ_PRODUCER_CHANNEL_POOL_SIZE = 100; // KNOB: the number of channels in a pool shared among write servlet threads, which is the producers of the messages to RMQ.
    public int RABBITMQ_CONSUMER_NUM_CONNECTIONS = 1; // KNOB(major): the number of connections from the consumer to rabbitmq, for each connection we create the same number of channels for each queue. In practice, 1 seems ok.
    public int RABBITMQ_CONSUMER_NUM_CHANNELS_PER_QUEUE = 5; // KNOB(major): the number of channels created for each queue, each channel will be handled by exactly one thread with exactly one connection to the DB. So the total number of consumer threads as well as DB connection is RABBITMQ_CONSUMER_NUM_CONNECTIONS*RABBITMQ_NUM_QUEUES*RABBITMQ_CONSUMER_NUM_CHANNELS_PER_QUEUE. This product heavily impacts the DB write performance.
    public int RABBITMQ_CONSUMER_NUM_WORKER_THREAD = 50; // KNOB(major): the number of worker threads inside RMQ that handles communication. You can create more channels than this value.
    public int RABBITMQ_CONSUMER_PREFETCH_COUNT = 20; // KNOB(major): prefect message count of a consumer channel.
    public int RABBITMQ_REQUEST_HEART_BEAT = 30; // Request heart beat in seconds

    public String MYSQL_READ_URL = "jdbc:mysql://localhost:3306/UPIC";
    public String MYSQL_WRITE_URL = "jdbc:mysql://localhost:3306/UPIC";
    public String MYSQL_USERNAME = "admin";
    public String MYSQL_PASSWORD = "adminadmin";
    public String MYSQL_TABLE_SCHEMA = "UPIC";
    public String MYSQL_INSERT_SQL = "INSERT INTO LiftRides (skier_id, resort_id, season_id, day_id, lift_id, ride_time) VALUES (?, ?, ?, ?, ?, ?)";
    public String MYSQL_GET_UNIQUE_SKIERS_SQL = "SELECT COUNT(DISTINCT skier_id) AS unique_skiers FROM LiftRides WHERE resort_id = ? AND season_id = ? AND day_id = ?";
    public String MYSQL_GET_DAILY_VERTICAL_SQL = "SELECT SUM(10 * lift_id) AS total_vertical FROM LiftRides WHERE skier_id = ? AND resort_id = ? AND season_id = ? AND day_id = ?";
    public String MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1 = "SELECT season_id, SUM(lift_id * 10) AS total_vertical FROM LiftRides WHERE skier_id = ? AND resort_id = ? AND season_id = ? GROUP BY season_id";
    public String MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2 = "SELECT season_id, SUM(lift_id * 10) AS total_vertical FROM LiftRides WHERE skier_id = ? AND resort_id = ? GROUP BY season_id";
    public int MYSQL_READ_MAX_POOL_SIZE = 16; // KNOB(major): the number of connections in a pool used in read service to read from the DB.
    public int MYSQL_WRITE_BATCH_SIZE = 100; // KNOB(major): the size of the batch in write service for insertion. Each batch of this size will be batch inserted into the DB.
    public int MYSQL_WRITE_FLUSH_INTERVAL_MS = 100; // KNOB(major): the time interval of batch insertion performed in write service. A batch will be inserted into the DB in two cases: 1. the batch size exceeds MYSQL_WRITE_BATCH_SIZE or 2. every FLUSH_INTERVAL.

    public String REDIS_HOST = "localhost";
    public int REDIS_PORT = 6379;
    public String REDIS_URL = "redis://localhost:6379";
    public String REDIS_KEY_PATTERN_UNIQUE_SKIERS = "resort:{resortID}:season:{seasonID}:day:{dayID}:unique_skiers";
    public String REDIS_KEY_PATTERN_DAILY_VERTICAL = "resort:{resortID}:season:{seasonID}:day:{dayID}:skier:{skierID}:vertical";
    public String REDIS_KEY_PATTERN_ALL_SEASON_VERTICALS = "skier:{skierID}:resort:{resortID}:all_verticals";
    public String REDIS_KEY_PATTERN_SINGLE_SEASON_VERTICAL = "skier:{skierId}:resort:{resortId}:season:{seasonId}:vertical";

    public boolean REDIS_BLOOM_FILTER_SWITCH = true; // KNOB: turn on bloom filter or not. Always recommend on.
    public int REDIS_BLOOM_FILTER_CAPACITY = 1000000;
    public double REDIS_BLOOM_FILTER_ERROR_RATE = 0.01;
    public String REDIS_BLOOM_FILTER_UNIQUE_SKIERS = "bloom:unique_skiers";
    public String REDIS_BLOOM_FILTER_DAILY_VERTICAL = "bloom:daily_vertical";
    public String REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS = "bloom:all_vertical";
    public String REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL = "bloom:season_vertical";

    public String LIFTRIDE_READ_SERVICE_HOST = "localhost";
    public int LIFTRIDE_READ_SERVICE_PORT = 8081;
    public int LIFTRIDE_READ_SERVICE_MIN_THREAD = 16; // KNOB: the minimum number of threads handling the incoming RPC requests in read service
    public int LIFTRIDE_READ_SERVICE_MAX_THREAD = 128;// KNOB: the maximum number of threads handling the incoming RPC requests in read service
    public int LIFTRIDE_READ_SERVICE_QUEUE_SIZE = 10000; // KNOB: the maximum number of incoming RPC requests that can be queued in read service

    public String AGGREGATION_FULL_ROW_COUNT_SQL = "SELECT COUNT(*) AS row_count FROM LiftRides";
    public String AGGREGATION_FULL_UNIQUE_SKIERS_SQL = "SELECT resort_id, season_id, day_id, COUNT(DISTINCT skier_id) as count FROM LiftRides GROUP BY resort_id, season_id, day_id";
    public String AGGREGATION_FULL_DAILY_VERTICAL_SQL = "SELECT resort_id, season_id, day_id, skier_id, SUM(10 * lift_id) as total_vertical FROM LiftRides GROUP BY resort_id, season_id, day_id, skier_id";
    public String AGGREGATION_FULL_SEASON_VERTICAL_SQL = "SELECT skier_id, resort_id, season_id, SUM(10 * lift_id) as total_vertical FROM LiftRides GROUP BY skier_id, resort_id, season_id";
    public String AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL = "SELECT resort_id, season_id, day_id FROM LiftRides GROUP BY resort_id, season_id, day_id";
    public String AGGREGATION_BLOOM_FILTER_KEY_DAILY_VERTICAL_SQL = "SELECT resort_id, season_id, day_id, skier_id FROM LiftRides GROUP BY resort_id, season_id, day_id, skier_id";
    public String AGGREGATION_BLOOM_FILTER_KEY_SINGLE_SEASON_VERTICAL_SQL = "SELECT skier_id, resort_id, season_id FROM LiftRides GROUP BY skier_id, resort_id, season_id";
    public String AGGREGATION_BLOOM_FILTER_KEY_ALL_SEASON_VERTICAL_SQL = "SELECT skier_id, resort_id FROM LiftRides GROUP BY skier_id, resort_id";
    public String AGGREGATION_HOT_KEY_UNIQUE_SKIERS = "resort:*:season:*:day:*:unique_skiers";
    public String AGGREGATION_HOT_KEY_DAILY_VERTICAL = "resort:*:season:*:day:*:skier:*:vertical";
    public String AGGREGATION_HOT_KEY_SINGLE_SEASON_VERTICAL = "skier:*:resort:*:season:*:vertical";
    public String AGGREGATION_HOT_KEY_ALL_SEASON_VERTICAL = "skier:*:resort:*:all_verticals";
    public long AGGREGATION_FULL_MAX_ROWS = 1000000L; // KNOB: the maximum number of rows in the DB that can perform full aggregation. If the number of rows in the DB is larger than this value, full aggregation won't execute.
    public int AGGREGATION_FULL_INTERVAL_SEC = 30; // KNOB: the time interval for every full aggregation
    public int AGGREGATION_BLOOM_ONLY_INTERVAL_SEC = 10; // KNOB: the time interval for every bloom filter aggregation
    public int AGGREGATION_REFRESH_CACHE_INTERVAL_SEC = 5; // KNOB(major): the time interval for every hot key cache refreshing aggregation

    public boolean RATE_LIMITER_READ_SWITCH = false; // KNOB: turn on rate limiter for read servlets or not
    public boolean RATE_LIMITER_WRITE_SWITCH = true; // KNOB: turn on rate limiter for write servlets or not
    public String RATE_LIMIT_READ_MODE = "LOCAL";
    public String RATE_LIMIT_WRITE_MODE = "LOCAL"; // e.g. "LOCAL", "REMOTE", or "REDIS"
    public int RATE_LIMITER_MAX_BACKOFF_MS = 2000; // KNOB: the maximum milliseconds of retry backoff(wait) time
    public int RATE_LIMITER_READ_MAX_TOKENS = 5000; // KNOB: the maximum number of tokens of read servlets
    public int RATE_LIMITER_READ_REFILL_RATE = 5000; // KNOB: the refill rate of tokens of read servlets, which is roughly the maximum number of throughput allowed
    public int RATE_LIMITER_WRITE_MAX_TOKENS = 5000; // KNOB: the maximum number of tokens of write servlets
    public int RATE_LIMITER_WRITE_REFILL_RATE = 5000; // KNOB: the refill rate of tokens of read servlets, which is roughly the maximum number of throughput/message production rate allowed
    public String RATE_LIMITER_READ_SERVLET_GROUP_ID = "r";
    public String RATE_LIMITER_WRITE_SERVLET_GROUP_ID = "w";
    public String RATE_LIMITER_SERVICE_HOST = "localhost";
    public int RATE_LIMITER_SERVICE_PORT = 9090;
    public int RATE_LIMITER_SERVICE_MIN_THREAD = 16; // the minimum number of threads handling the incoming RPC requests in rate limiter service
    public int RATE_LIMITER_SERVICE_MAX_THREAD = 128; // the maximum number of threads handling the incoming RPC requests in rate limiter service
    public int RATE_LIMITER_SERVICE_QUEUE_SIZE = 10000; // the maximum number of incoming RPC requests that can be queued in rate limiter service

    public int MAX_RETRIES = 5; // Maximum number of retries for all operations

    public String REDIS_KEY_UNIQUE_SKIER_COUNT;
    public String REDIS_KEY_VERTICAL_WITH_SKIER;
    public String REDIS_KEY_VERTICAL_COUNT;

    public Configuration() {}

    public Configuration(Properties properties) {
        String local = properties.getProperty("local");
        if (local.equals("true")) {
            RABBITMQ_HOST = "localhost";
            REDIS_HOST = "localhost";
            LIFTRIDE_READ_SERVICE_HOST = "localhost";
            RATE_LIMITER_SERVICE_HOST = "localhost";
        } else {
            RABBITMQ_HOST = properties.getProperty("RABBITMQ_HOST");
            REDIS_HOST = properties.getProperty("REDIS_HOST");
            LIFTRIDE_READ_SERVICE_HOST = properties.getProperty("LIFTRIDE_READ_SERVICE_HOST");
            RATE_LIMITER_SERVICE_HOST = properties.getProperty("RATE_LIMITER_SERVICE_HOST");
        }

        RABBITMQ_USERNAME = properties.getProperty("RABBITMQ_USERNAME");
        RABBITMQ_PASSWORD = properties.getProperty("RABBITMQ_PASSWORD");
        RABBITMQ_EXCHANGE_NAME = properties.getProperty("RABBITMQ_EXCHANGE_NAME");
        RABBITMQ_ROUTING_KEY = properties.getProperty("RABBITMQ_ROUTING_KEY");
        RABBITMQ_QUEUE_NAME_PREFIX = properties.getProperty("RABBITMQ_QUEUE_NAME_PREFIX");
        RABBITMQ_MAX_QUEUED_MSG = Integer.parseInt(properties.getProperty("RABBITMQ_MAX_QUEUED_MSG"));
        RABBITMQ_CIRCUIT_BREAKER_THRESHOLD = Integer.parseInt(properties.getProperty("RABBITMQ_CIRCUIT_BREAKER_THRESHOLD"));
        RABBITMQ_CIRCUIT_BREAKER_TIMEOUT_MS = Integer.parseInt(properties.getProperty("RABBITMQ_CIRCUIT_BREAKER_TIMEOUT_MS"));
        RABBITMQ_QUEUE_MONITOR_THREAD_COUNT = Integer.parseInt(properties.getProperty("RABBITMQ_QUEUE_MONITOR_THREAD_COUNT"));
        RABBITMQ_QUEUE_MONITOR_INTERVAL_MS = Integer.parseInt(properties.getProperty("RABBITMQ_QUEUE_MONITOR_INTERVAL_MS"));
        RABBITMQ_NUM_QUEUES = Integer.parseInt(properties.getProperty("RABBITMQ_NUM_QUEUES"));
        RABBITMQ_CONSUMER_NUM_CONNECTIONS = Integer.parseInt(properties.getProperty("RABBITMQ_CONSUMER_NUM_CONNECTIONS"));
        RABBITMQ_PRODUCER_CHANNEL_POOL_SIZE = Integer.parseInt(properties.getProperty("RABBITMQ_PRODUCER_CHANNEL_POOL_SIZE"));
        RABBITMQ_REQUEST_HEART_BEAT = Integer.parseInt(properties.getProperty("RABBITMQ_REQUEST_HEART_BEAT"));
        RABBITMQ_CONSUMER_PREFETCH_COUNT = Integer.parseInt(properties.getProperty("RABBITMQ_CONSUMER_PREFETCH_COUNT"));
        RABBITMQ_CONSUMER_NUM_CHANNELS_PER_QUEUE = Integer.parseInt(properties.getProperty("RABBITMQ_CONSUMER_NUM_CHANNELS_PER_QUEUE"));
        RABBITMQ_CONSUMER_NUM_WORKER_THREAD = Integer.parseInt(properties.getProperty("RABBITMQ_CONSUMER_NUM_WORKER_THREAD"));

        MYSQL_READ_URL = properties.getProperty("MYSQL_READ_URL");
        MYSQL_WRITE_URL = properties.getProperty("MYSQL_WRITE_URL");
        MYSQL_USERNAME = properties.getProperty("MYSQL_USERNAME");
        MYSQL_PASSWORD = properties.getProperty("MYSQL_PASSWORD");
        MYSQL_TABLE_SCHEMA = properties.getProperty("MYSQL_TABLE_SCHEMA");
        MYSQL_INSERT_SQL = properties.getProperty("MYSQL_INSERT_SQL");

        MYSQL_GET_UNIQUE_SKIERS_SQL = properties.getProperty("MYSQL_GET_UNIQUE_SKIERS_SQL");
        MYSQL_GET_DAILY_VERTICAL_SQL = properties.getProperty("MYSQL_GET_DAILY_VERTICAL_SQL");
        MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1 = properties.getProperty("MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_1");
        MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2 = properties.getProperty("MYSQL_GET_TOTAL_VERTICAL_SQL_CASE_2");

        MYSQL_READ_MAX_POOL_SIZE = Integer.parseInt(properties.getProperty("MYSQL_READ_MAX_POOL_SIZE"));
        MYSQL_WRITE_BATCH_SIZE = Integer.parseInt(properties.getProperty("MYSQL_WRITE_BATCH_SIZE"));
        MYSQL_WRITE_FLUSH_INTERVAL_MS = Integer.parseInt(properties.getProperty("MYSQL_WRITE_FLUSH_INTERVAL_MS"));

        REDIS_PORT = Integer.parseInt(properties.getProperty("REDIS_PORT"));
        REDIS_URL = properties.getProperty("REDIS_URL");
        REDIS_KEY_PATTERN_UNIQUE_SKIERS = properties.getProperty("REDIS_KEY_PATTERN_UNIQUE_SKIERS");
        REDIS_KEY_PATTERN_DAILY_VERTICAL = properties.getProperty("REDIS_KEY_PATTERN_DAILY_VERTICAL");
        REDIS_KEY_PATTERN_ALL_SEASON_VERTICALS = properties.getProperty("REDIS_KEY_PATTERN_ALL_SEASON_VERTICALS");
        REDIS_KEY_PATTERN_SINGLE_SEASON_VERTICAL = properties.getProperty("REDIS_KEY_PATTERN_SINGLE_SEASON_VERTICAL");

        REDIS_BLOOM_FILTER_UNIQUE_SKIERS = properties.getProperty("REDIS_BLOOM_FILTER_UNIQUE_SKIERS");
        REDIS_BLOOM_FILTER_DAILY_VERTICAL = properties.getProperty("REDIS_BLOOM_FILTER_DAILY_VERTICAL");
        REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS = properties.getProperty("REDIS_BLOOM_FILTER_ALL_SEASON_VERTICALS");
        REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL = properties.getProperty("REDIS_BLOOM_FILTER_SINGLE_SEASON_VERTICAL");

        REDIS_BLOOM_FILTER_SWITCH = Boolean.parseBoolean(properties.getProperty("REDIS_BLOOM_FILTER_SWITCH"));
        REDIS_BLOOM_FILTER_CAPACITY = Integer.parseInt(properties.getProperty("REDIS_BLOOM_FILTER_CAPACITY"));
        REDIS_BLOOM_FILTER_ERROR_RATE = Double.parseDouble(properties.getProperty("REDIS_BLOOM_FILTER_ERROR_RATE"));

        LIFTRIDE_READ_SERVICE_PORT = Integer.parseInt(properties.getProperty("LIFTRIDE_READ_SERVICE_PORT"));
        LIFTRIDE_READ_SERVICE_MIN_THREAD = Integer.parseInt(properties.getProperty("LIFTRIDE_READ_SERVICE_MIN_THREAD"));
        LIFTRIDE_READ_SERVICE_MAX_THREAD = Integer.parseInt(properties.getProperty("LIFTRIDE_READ_SERVICE_MAX_THREAD"));
        LIFTRIDE_READ_SERVICE_QUEUE_SIZE = Integer.parseInt(properties.getProperty("LIFTRIDE_READ_SERVICE_QUEUE_SIZE"));

        AGGREGATION_FULL_ROW_COUNT_SQL = properties.getProperty("AGGREGATION_FULL_ROW_COUNT_SQL");
        AGGREGATION_FULL_UNIQUE_SKIERS_SQL = properties.getProperty("AGGREGATION_FULL_UNIQUE_SKIERS_SQL");
        AGGREGATION_FULL_DAILY_VERTICAL_SQL = properties.getProperty("AGGREGATION_FULL_DAILY_VERTICAL_SQL");
        AGGREGATION_FULL_SEASON_VERTICAL_SQL = properties.getProperty("AGGREGATION_FULL_SEASON_VERTICAL_SQL");
        AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL = properties.getProperty("AGGREGATION_BLOOM_FILTER_KEY_UNIQUE_SKIERS_SQL");
        AGGREGATION_BLOOM_FILTER_KEY_DAILY_VERTICAL_SQL = properties.getProperty("AGGREGATION_BLOOM_FILTER_KEY_DAILY_VERTICAL_SQL");
        AGGREGATION_BLOOM_FILTER_KEY_SINGLE_SEASON_VERTICAL_SQL = properties.getProperty("AGGREGATION_BLOOM_FILTER_KEY_SINGLE_SEASON_VERTICAL_SQL");
        AGGREGATION_BLOOM_FILTER_KEY_ALL_SEASON_VERTICAL_SQL = properties.getProperty("AGGREGATION_BLOOM_FILTER_KEY_ALL_SEASON_VERTICAL_SQL");
        AGGREGATION_HOT_KEY_UNIQUE_SKIERS = properties.getProperty("AGGREGATION_HOT_KEY_UNIQUE_SKIERS");
        AGGREGATION_HOT_KEY_DAILY_VERTICAL = properties.getProperty("AGGREGATION_HOT_KEY_DAILY_VERTICAL");
        AGGREGATION_HOT_KEY_SINGLE_SEASON_VERTICAL = properties.getProperty("AGGREGATION_HOT_KEY_SINGLE_SEASON_VERTICAL");
        AGGREGATION_HOT_KEY_ALL_SEASON_VERTICAL = properties.getProperty("AGGREGATION_HOT_KEY_ALL_SEASON_VERTICAL");

        AGGREGATION_FULL_MAX_ROWS = Long.parseLong(properties.getProperty("AGGREGATION_FULL_MAX_ROWS"));
        AGGREGATION_FULL_INTERVAL_SEC = Integer.parseInt(properties.getProperty("AGGREGATION_FULL_INTERVAL_SEC"));
        AGGREGATION_BLOOM_ONLY_INTERVAL_SEC = Integer.parseInt(properties.getProperty("AGGREGATION_BLOOM_ONLY_INTERVAL_SEC"));
        AGGREGATION_REFRESH_CACHE_INTERVAL_SEC = Integer.parseInt(properties.getProperty("AGGREGATION_REFRESH_CACHE_INTERVAL_SEC"));

        RATE_LIMITER_MAX_BACKOFF_MS = Integer.parseInt(properties.getProperty("RATE_LIMITER_MAX_BACKOFF_MS"));
        RATE_LIMITER_READ_SWITCH = Boolean.parseBoolean(properties.getProperty("RATE_LIMITER_READ_SWITCH"));
        RATE_LIMITER_WRITE_SWITCH = Boolean.parseBoolean(properties.getProperty("RATE_LIMITER_WRITE_SWITCH"));
        RATE_LIMIT_READ_MODE = properties.getProperty("RATE_LIMITER_READ_MODE");
        RATE_LIMIT_WRITE_MODE = properties.getProperty("RATE_LIMITER_WRITE_MODE");
        RATE_LIMITER_WRITE_SERVLET_GROUP_ID = properties.getProperty("RATE_LIMITER_WRITE_SERVLET_GROUP_ID");
        RATE_LIMITER_READ_SERVLET_GROUP_ID = properties.getProperty("RATE_LIMITER_READ_SERVLET_GROUP_ID");
        RATE_LIMITER_SERVICE_PORT = Integer.parseInt(properties.getProperty("RATE_LIMITER_SERVICE_PORT"));
        RATE_LIMITER_SERVICE_MIN_THREAD = Integer.parseInt(properties.getProperty("RATE_LIMITER_SERVICE_MIN_THREAD"));
        RATE_LIMITER_SERVICE_MAX_THREAD = Integer.parseInt(properties.getProperty("RATE_LIMITER_SERVICE_MAX_THREAD"));
        RATE_LIMITER_SERVICE_QUEUE_SIZE = Integer.parseInt(properties.getProperty("RATE_LIMITER_SERVICE_QUEUE_SIZE"));
        RATE_LIMITER_READ_MAX_TOKENS = Integer.parseInt(properties.getProperty("RATE_LIMITER_READ_MAX_TOKENS"));
        RATE_LIMITER_READ_REFILL_RATE = Integer.parseInt(properties.getProperty("RATE_LIMITER_READ_REFILL_RATE"));
        RATE_LIMITER_WRITE_MAX_TOKENS = Integer.parseInt(properties.getProperty("RATE_LIMITER_WRITE_MAX_TOKENS"));
        RATE_LIMITER_WRITE_REFILL_RATE = Integer.parseInt(properties.getProperty("RATE_LIMITER_WRITE_REFILL_RATE"));



        MAX_RETRIES = Integer.parseInt(properties.getProperty("MAX_RETRIES"));


        REDIS_KEY_UNIQUE_SKIER_COUNT = properties.getProperty("REDIS_KEY_UNIQUE_SKIER_COUNT");
        REDIS_KEY_VERTICAL_WITH_SKIER= properties.getProperty("REDIS_KEY_VERTICAL_WITH_SKIER");
        REDIS_KEY_VERTICAL_COUNT = properties.getProperty("REDIS_KEY_VERTICAL_COUNT");
    }
}
