package edu.northeastern.clientpart2;

import edu.northeastern.common.CsvWriterThread;
import edu.northeastern.common.SkiersApiLoadTestSync;
import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

import java.io.IOException;

public class Client2Sync {
    public static void main(String[] args) throws InterruptedException, IOException {
        Configuration config = new Configuration(true, true);
        ConsumerContext context = new ConsumerContext(null, config);
        // CSV Writer thread
        Thread csvWriterThread = new Thread(new CsvWriterThread(context.metricsBuffer, config.CSV_FILE));
        csvWriterThread.start();
        SkiersApiLoadTestSync.postLoadTestThreadPool(config, context);
        // Signal the CSV writer to finish
        context.metricsBuffer.put(new String[]{"EOF"});
        csvWriterThread.join();
        CsvWriterThread.calculateMetrics(config.CSV_FILE);
    }
}
