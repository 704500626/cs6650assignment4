package edu.northeastern.model;

import edu.northeastern.utils.ConfigUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    // Global constants for the load test
    public int TOTAL_REQUEST_COUNT = 200_000; // Total number of POST requests to send to the server
    public int PHASE1_THREAD_COUNT = 32; // The number of consumer threads sending requests for phase 1
    public int PHASE1_PER_THREAD_REQUEST_COUNT = 1000; // The number of POST requests to be sent by consumer threads of phase 1
    public int PHASE2_THREAD_COUNT = 1; // The number of consumer threads sending requests for phase 2
    public int REQUEST_BUFFER_SIZE = 1000; // The buffer size of the http request content, kept as small as possible
    public int METRICS_BUFFER_SIZE = 5000; // The buffer size of metrics queued
    public int MAX_CONCURRENT_REQUESTS = 1200; // The maximum number of concurrent requests
    public int MAX_RETRIES = 5; // The maximum number of retries of each request
    public String serverUrl = ConfigUtils.getServerUrl();
    public String CSV_FILE = "request_metrics.csv";
    public boolean storeMetrics = false;
    public boolean sync = true;

    public Configuration() {
        this(false, false);
    }

    public Configuration(boolean sync, boolean storeMetrics) {
        Properties properties = new Properties();
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            TOTAL_REQUEST_COUNT = Integer.parseInt(properties.getProperty("TOTAL_REQUEST_COUNT"));
            PHASE1_THREAD_COUNT = Integer.parseInt(properties.getProperty("PHASE1_THREAD_COUNT"));
            PHASE1_PER_THREAD_REQUEST_COUNT = Integer.parseInt(properties.getProperty("PHASE1_PER_THREAD_REQUEST_COUNT"));
            if (sync)
                PHASE2_THREAD_COUNT = Integer.parseInt(properties.getProperty("PHASE2_THREAD_COUNT_SYNC"));
            else
                PHASE2_THREAD_COUNT = Integer.parseInt(properties.getProperty("PHASE2_THREAD_COUNT_ASYNC"));
            REQUEST_BUFFER_SIZE = Integer.parseInt(properties.getProperty("REQUEST_BUFFER_SIZE"));
            METRICS_BUFFER_SIZE = Integer.parseInt(properties.getProperty("METRICS_BUFFER_SIZE"));
            MAX_CONCURRENT_REQUESTS = Integer.parseInt(properties.getProperty("MAX_CONCURRENT_REQUESTS"));
            MAX_RETRIES = Integer.parseInt(properties.getProperty("MAX_RETRIES"));
            CSV_FILE = properties.getProperty("CSV_FILE");
            this.storeMetrics = storeMetrics;
            this.sync = sync;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
