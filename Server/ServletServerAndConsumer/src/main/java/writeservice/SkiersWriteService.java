package writeservice;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.*;
import dao.LiftRideWriter;
import model.Configuration;
import model.LiftRideEventMsg;
import utils.ConfigUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SkiersWriteService {
    private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();
    private static final Gson gson = new Gson();
    private static final List<Connection> mqConnections = new ArrayList<>();
    // Global list to hold all DBWriter instances
    private static final List<LiftRideWriter> dbWriters = new ArrayList<>();
    // Shared flush scheduler for all DBWriters
    private static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {
        setupRabbitMQConnections();
        setupFlushScheduler();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanup();
            flushScheduler.shutdown();
        }));
    }

    // Sets up RabbitMQ connection (only one connection is shared)
    private static void setupRabbitMQConnections() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setSharedExecutor(Executors.newFixedThreadPool(
                config.NUM_CONNECTIONS * config.NUM_QUEUES * config.NUM_CHANNELS_PER_QUEUE));
        factory.setHost(config.RABBITMQ_HOST);
        factory.setUsername(config.RABBITMQ_USERNAME);
        factory.setPassword(config.RABBITMQ_PASSWORD);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setRequestedHeartbeat(30);

        for (int i = 0; i < config.NUM_CONNECTIONS; i++) {
            mqConnections.add(factory.newConnection());
        }

        for (Connection conn : mqConnections) {
            try (Channel setupChannel = conn.createChannel()) {
                for (int q = 0; q < config.NUM_QUEUES; q++) {
                    String queueName = config.EXCHANGE_NAME + "_queue_" + q;
                    setupChannel.queueDeclare(queueName, true, false, false, null);
                    for (int c = 0; c < config.NUM_CHANNELS_PER_QUEUE; c++) {
                        try {
                            consumeMessages(conn, queueName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        System.out.println("RabbitMQ Consumer connected!");
    }

    private static void setupFlushScheduler() {
        flushScheduler.scheduleAtFixedRate(() -> {
            for (LiftRideWriter writer : dbWriters) {
                try {
                    writer.flush();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, config.MYSQL_FLUSH_INTERVAL_MS, config.MYSQL_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // Consumer function that continuously listens for messages
    private static void consumeMessages(Connection connection, String queueName) throws IOException, TimeoutException {
        Channel channel = connection.createChannel();
        channel.basicQos(config.PREFETCH_COUNT);

        LiftRideWriter dbWriter;
        try {
            dbWriter = new LiftRideWriter(config, channel);
            dbWriters.add(dbWriter);
        } catch (SQLException e) {
            channel.close();
            throw new RuntimeException("Failed to initialize DBWriter", e);
        }

        DeliverCallback callback = (consumerTag, delivery) -> {
            String msgJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                LiftRideEventMsg event = gson.fromJson(msgJson, LiftRideEventMsg.class);
                dbWriter.addEvent(event, delivery.getEnvelope().getDeliveryTag());
            } catch (JsonSyntaxException e) {
                System.err.println("Malformed JSON: " + msgJson);
                try {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, callback, consumerTag -> {});
    }

    private static void cleanup() {
        for (Connection conn : mqConnections) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("RabbitMQ connection shutting down.");
        for (LiftRideWriter writer : dbWriters) {
            writer.close();
        }
        System.out.println("MySQL connection shutting down.");
    }
}
