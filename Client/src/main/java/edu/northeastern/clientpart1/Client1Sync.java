package edu.northeastern.clientpart1;

import edu.northeastern.common.SkiersApiLoadTestSync;
import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

public class Client1Sync {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration(true, false);
        ConsumerContext context = new ConsumerContext(null, config);
        SkiersApiLoadTestSync.postLoadTestThreadPool(config, context);
    }
}
