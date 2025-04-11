package edu.northeastern.common;

import edu.northeastern.model.Configuration;
import edu.northeastern.model.ConsumerContext;

public abstract class ConsumerThread implements Runnable {
    protected final Configuration configuration;
    protected final ConsumerContext context;

    public ConsumerThread(Configuration configuration, ConsumerContext context) {
        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public void run() {}
}