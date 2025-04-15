package ratelimiter.service;

import grpc.RateLimiterServiceGrpc;
import grpc.RateLimiterServiceProto.*;
import io.grpc.stub.StreamObserver;
import ratelimiter.algorithms.TokenBucketRateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterServiceImpl extends RateLimiterServiceGrpc.RateLimiterServiceImplBase {
    private final Map<String, TokenBucketRateLimiter> limiters = new ConcurrentHashMap<>();
    private final int maxTokens;
    private final int refillRate;

    public RateLimiterServiceImpl(int maxTokens, int refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
    }

    @Override
    public void allow(RateLimiterRequest request, StreamObserver<RateLimiterResponse> responseObserver) {
        String groupId = request.getGroupId();
        limiters.putIfAbsent(groupId, new TokenBucketRateLimiter(maxTokens, refillRate));
        boolean allowed = limiters.get(groupId).allowRequest();
        responseObserver.onNext(RateLimiterResponse.newBuilder().setAllowed(allowed).build());
        responseObserver.onCompleted();
    }
}
