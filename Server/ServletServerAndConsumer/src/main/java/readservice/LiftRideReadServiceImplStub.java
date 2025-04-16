package readservice;

import cache.RedisCacheClient;
import dao.LiftRideReader;
import grpc.LiftRideReadProto.*;
import grpc.SkierReadServiceGrpc;
import io.grpc.stub.StreamObserver;
import model.Configuration;

import java.util.List;

public class LiftRideReadServiceImplStub extends SkierReadServiceGrpc.SkierReadServiceImplBase {
    private final LiftRideReader dbReader;
    private final RedisCacheClient cache;
    private final Configuration config;

    public LiftRideReadServiceImplStub(Configuration config) {
        this.config = config;
        this.dbReader = new LiftRideReader(config);
        this.cache = new RedisCacheClient(config);
    }

    @Override
    public void getTotalVertical(VerticalRequest request, StreamObserver<VerticalListResponse> responseObserver) {
        responseObserver.onNext(VerticalListResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getSkierDayRides(SkierDayRequest request, StreamObserver<VerticalIntResponse> responseObserver) {
        responseObserver.onNext(VerticalIntResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getResortDaySkiers(ResortDayRequest request, StreamObserver<SkierCountResponse> responseObserver) {
        responseObserver.onNext(SkierCountResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void close() {
        dbReader.close();
        cache.close();
    }
}