package ratelimiter;

import grpc.RateLimiterServiceGrpc;
import grpc.RateLimiterServiceProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class RemoteRateLimiter implements RateLimiter {
    private final RateLimiterServiceGrpc.RateLimiterServiceBlockingStub stub;
    private final String groupId;

    public RemoteRateLimiter(String serverHost, int serverPort, String groupId) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverHost, serverPort)
                .usePlaintext()
                .build();
        this.stub = RateLimiterServiceGrpc.newBlockingStub(channel);
        this.groupId = groupId;
    }

    @Override
    public boolean allowRequest() {
        RateLimiterRequest request = RateLimiterRequest.newBuilder().setGroupId(groupId).build();
        RateLimiterResponse response = stub.allow(request);
        return response.getAllowed();
    }

    @Override
    public boolean allowRequestWithRetries(int maxRetries, int maxBackoffMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (allowRequest()) return true;
            try {
                Thread.sleep(Math.min(100 * (i + 1), maxBackoffMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
