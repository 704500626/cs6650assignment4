package servlet;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import messagequeue.RMQChannelFactory;
import messagequeue.RMQChannelPool;
import model.Configuration;
import model.LiftRideEventMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RabbitMQPublisher {
    private static final Gson gson = new Gson();
    private final Configuration config;
    private final RMQChannelPool channelPool;
    private final List<String> queueNames = new ArrayList<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    // Queue monitoring and circuit breaker fields
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService monitorPool;
    private volatile int currentQueueDepth = 0;
    private volatile boolean circuitOpen = false;
    private volatile long circuitOpenedTime = 0;

    public RabbitMQPublisher(Configuration config) throws Exception {
        this.config = config;
        for (int i = 0; i < config.NUM_QUEUES; i++) {
            queueNames.add(config.EXCHANGE_NAME + "_queue_" + i);
        }
        monitorPool = Executors.newFixedThreadPool(config.QUEUE_MONITOR_THREAD_COUNT);
        for (int q = 0; q < config.NUM_QUEUES; q++) {
            queueNames.add(config.EXCHANGE_NAME + "_queue_" + q);
        }
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.RABBITMQ_HOST);
            factory.setUsername(config.RABBITMQ_USERNAME);
            factory.setPassword(config.RABBITMQ_PASSWORD);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setRequestedHeartbeat(config.REQUEST_HEART_BEAT);
            Connection connection = factory.newConnection();
            channelPool = new RMQChannelPool(config.CHANNEL_POOL_SIZE, new RMQChannelFactory(connection));
            try (Channel setupChannel = connection.createChannel()) {
                setupChannel.exchangeDeclare(config.EXCHANGE_NAME, "direct", true);
                for (int q = 0; q < config.NUM_QUEUES; q++) {
                    String queueName = queueNames.get(q);
                    setupChannel.queueDeclare(queueName, true, false, false, null);
                    setupChannel.queueBind(queueName, config.EXCHANGE_NAME, queueName);
                }
            }
            startQueueMonitoring();
        } catch (Exception e) {
            throw new Exception("Failed to initialize RabbitMQ connection", e);
        }
    }

    private void startQueueMonitoring() {
        scheduler.scheduleAtFixedRate(this::monitorQueues, 0, config.QUEUE_MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void monitorQueues() {
        List<Future<Integer>> futures = new ArrayList<>();
        for (String queueName : queueNames) {
            futures.add(monitorPool.submit(() -> getQueueDepth(queueName)));
        }
        int totalDepth = 0;
        for (Future<Integer> future : futures) {
            try {
                totalDepth += future.get();
            } catch (Exception e) {
                totalDepth += config.MAX_QUEUED_MSG;
            }
        }
        currentQueueDepth = totalDepth;
        // Circuit breaker logic
        if (currentQueueDepth > config.CIRCUIT_BREAKER_THRESHOLD && !circuitOpen) {
            circuitOpen = true;
            circuitOpenedTime = System.currentTimeMillis();
            System.err.println("[WARN] Circuit opened: Queue depth (" + currentQueueDepth + ") exceeded threshold!");
        } else if (circuitOpen && (System.currentTimeMillis() - circuitOpenedTime > config.CIRCUIT_BREAKER_TIMEOUT_MS)) {
            circuitOpen = false;
            System.out.println("[INFO] Circuit closed: Resuming normal operations.");
        }
    }

    private int getQueueDepth(String queueName) {
        Channel channel = null;
        int depth = config.MAX_QUEUED_MSG;
        try {
            channel = channelPool.borrowObject();
            depth = channel.queueDeclarePassive(queueName).getMessageCount();
        } catch (IOException e) {
            System.err.println("Error fetching queue depth for queue [" + queueName + "]: " + e.getMessage());
        } finally {
            if (channel != null) {
                try {
                    channelPool.returnObject(channel);
                } catch (Exception ex) {
                    System.err.println("Error returning channel: " + ex.getMessage());
                }
            }
        }
        return depth;
    }

    /**
     * Returns true if the circuit is open (i.e. the queue depth is too high).
     */
    public boolean isCircuitOpen() {
        return circuitOpen;
    }

    /**
     * Waits (with retry/backoff) until the current queue depth is below the configured maximum.
     * Returns true if acceptable within the retries; false otherwise.
     */
    public boolean waitForAcceptableQueueDepth(int maxRetries, int maxBackoffMs) {
        int retries = 0;
        while (currentQueueDepth > config.MAX_QUEUED_MSG && retries < maxRetries) {
            try {
                int delayMs = Math.min(100 * (retries + 1), maxBackoffMs);
                System.out.println("Queue depth high (" + currentQueueDepth + "), delaying by " + delayMs + "ms");
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            retries++;
        }
        return retries < maxRetries;
    }

    /**
     * Publishes a LiftRideEventMsg to RabbitMQ using a round-robin queue selection.
     */
    public boolean publishLiftRideEvent(LiftRideEventMsg event) throws Exception {
        String messageJson = gson.toJson(event);
        int queueIndex = requestCounter.getAndIncrement() % config.NUM_QUEUES;
        Channel channel = null;
        try {
            channel = channelPool.borrowObject();
            channel.basicPublish(config.EXCHANGE_NAME, queueNames.get(queueIndex), MessageProperties.PERSISTENT_TEXT_PLAIN, messageJson.getBytes());
        } catch (Exception e) {
            return false;
        } finally {
            if (channel != null) {
                try {
                    channelPool.returnObject(channel);
                } catch (Exception ex) {
                    System.err.println("Error returning channel: " + ex.getMessage());
                }
            }
        }
        return true;
    }

    public void shutdown() {
        try {
            channelPool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        scheduler.shutdown();
        monitorPool.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!monitorPool.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            monitorPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}