package ratelimiter.algorithms;

import ratelimiter.RateLimiter;

public class LeakyBucketRateLimiter implements RateLimiter {
    private final long capacity;         // Maximum capacity of the bucket
    private final double leakIntervalMs; // Interval (in ms) between leaking 1 unit
    private long lastLeakTime;           // Timestamp of last leak
    private long waterLevel;             // Current water level

    /**
     * @param capacity          The max number of requests (water level) the bucket can hold.
     * @param leakRatePerSecond The rate at which requests are processed (units per second).
     */
    public LeakyBucketRateLimiter(long capacity, double leakRatePerSecond) {
        this.capacity = capacity;
        // e.g., if leakRatePerSecond = 500, leakIntervalMs = 2.0 ms per leak
        this.leakIntervalMs = 1000.0 / leakRatePerSecond;
        this.lastLeakTime = System.currentTimeMillis();
        this.waterLevel = 0;
    }

    @Override
    public synchronized boolean allowRequest() {
        leakWater();  // Leak water before adding a new request
        if (waterLevel < capacity) {
            waterLevel++;
            return true;
        }
        // Bucket is full, request denied
        return false;
    }

    /**
     * Leaks water based on how much time has elapsed since the last leak.
     */
    private void leakWater() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastLeakTime;
        if (elapsedTime <= 0) {
            return; // No time has passed, no leak
        }
        // How many units of water can leak in 'elapsedTime'?
        double leakedUnits = elapsedTime / leakIntervalMs; // This is now a double calculation
        if (leakedUnits < 1.0) {
            // Not enough time has passed to leak even one unit
            return;
        }
        // Convert to a long number of units to leak
        long leakCount = (long) leakedUnits;

        // Decrease water level by that many units
        waterLevel = Math.max(0, waterLevel - leakCount);

        // Advance lastLeakTime by the amount of time corresponding to 'leakCount' leaked units
        // so that we don't "double count" the same elapsed time in the next call
        long leakTimeAdvance = (long) (leakCount * leakIntervalMs);
        lastLeakTime += leakTimeAdvance;
        // Edge case: If we've leaked enough that the new time is still behind 'now'
        // (because we truncated leakUnits to a long), the next call will leak more
        // for the remainder. This is okay and keeps the logic simpler.
    }

    @Override
    public boolean allowRequestWithRetries(int maxRetries, int maxBackoffMs) {
        return false; // TODO
    }
}