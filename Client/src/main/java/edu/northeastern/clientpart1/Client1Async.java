package edu.northeastern.clientpart1;

import edu.northeastern.common.SkiersApiLoadTestAsync;
import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client1Async {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration(false, false);
        // An executor with a fixed thread pool for the async client
        ExecutorService executor = Executors.newFixedThreadPool(config.MAX_CONCURRENT_REQUESTS);
        ConsumerContext context = new ConsumerContext(executor, config);
        SkiersApiLoadTestAsync.postLoadTest(config, context);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
