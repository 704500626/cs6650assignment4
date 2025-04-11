package edu.northeastern.clientpart2;

import edu.northeastern.common.CsvWriterThread;
import edu.northeastern.common.SkiersApiLoadTestAsync;
import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client2Async {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration(false, true);
        // An executor with a fixed thread pool for the async client
        ExecutorService executor = Executors.newFixedThreadPool(config.MAX_CONCURRENT_REQUESTS);
        ConsumerContext context = new ConsumerContext(executor, config);
        // CSV Writer thread
        Thread csvWriterThread = new Thread(new CsvWriterThread(context.metricsBuffer, config.CSV_FILE));
        csvWriterThread.start();
        SkiersApiLoadTestAsync.postLoadTest(config, context);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        // Signal the CSV writer to finish
        context.metricsBuffer.put(new String[]{"EOF"});
        csvWriterThread.join();
    }
}
