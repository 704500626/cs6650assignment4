package model;

public enum CacheHitLevel {
    BLOOM_NEGATIVE,
    LRU_HIT,
    REDIS_HIT,
    DB_HIT
}
