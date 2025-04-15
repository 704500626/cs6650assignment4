package batch;

// Shared interface for aggregation strategies
public interface AggregationStrategy {
    void run() throws Exception;
    void close();
}

