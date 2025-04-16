package batch;

import bloomfilter.BloomFilterUtils;
import bloomfilter.LiftRideBloomFilter;
import com.google.protobuf.ByteString;
import grpc.BatchAggregationServiceGrpc;
import grpc.BatchAggregationServiceProto.BloomFilterSnapshot;
import grpc.BatchAggregationServiceProto.Empty;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class BatchAggregationServiceImpl extends BatchAggregationServiceGrpc.BatchAggregationServiceImplBase {
    private final LiftRideBloomFilter bloomFilter;

    public BatchAggregationServiceImpl(LiftRideBloomFilter bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    @Override
    public void getBloomFilterSnapshot(Empty request, StreamObserver<BloomFilterSnapshot> responseObserver) {
        try {
            BloomFilterSnapshot response = BloomFilterSnapshot.newBuilder().setUniqueSkiersFilter(ByteString.copyFrom(BloomFilterUtils.serializeAndCompress(bloomFilter.getUniqueSkiersFilter()))).setDailyVerticalFilter(ByteString.copyFrom(BloomFilterUtils.serializeAndCompress(bloomFilter.getDailyVerticalFilter()))).setSeasonVerticalFilter(ByteString.copyFrom(BloomFilterUtils.serializeAndCompress(bloomFilter.getSeasonVerticalFilter()))).setTotalVerticalFilter(ByteString.copyFrom(BloomFilterUtils.serializeAndCompress(bloomFilter.getTotalVerticalFilter()))).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }
}