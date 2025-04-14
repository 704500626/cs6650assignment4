package readservice;

import cacheservice.CacheReadService;
import cacheservice.CacheWriteService;
import dao.LiftRideReader;
import io.grpc.stub.StreamObserver;
import java.sql.SQLException;
import model.Configuration;
import skierread.SkierReadServiceGrpc;
import skierread.SkierReadServiceOuterClass.ResortDayRequest;
import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.SkierDayRequest;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;
import skierread.SkierReadServiceOuterClass.VerticalRequest;
import utils.ConfigUtils;

public class SkierReadServiceImpl extends SkierReadServiceGrpc.SkierReadServiceImplBase {

  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  private final LiftRideReader liftRideReader = new LiftRideReader(config);

  @Override
  public void getTotalVertical(VerticalRequest request,
      StreamObserver<VerticalListResponse> responseObserver) {
    VerticalListResponse response = null;
    // Check if in the Redis Cache
    response = CacheReadService.getTotalVerticalFromCache(request.getResortID(), request.getSeasonID(),
        request.getSkierID());
    if (response == null) { // if Not in Redis, get from the DB and then write to Redis
      try {
        if (request.getSeasonID().isEmpty()) {
          response = liftRideReader.getSkierResortTotals(request.getSkierID(),
              request.getResortID());
        } else {
          response = liftRideReader.getSkierResortTotals(request.getSkierID(),
              request.getResortID(), request.getSeasonID());
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      // write to Redis
      CacheWriteService.writeVerticalListToCache(request.getSkierID(),
          request.getResortID(),
          response.getRecordsList());
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getSkierDayRides(SkierDayRequest request,
      StreamObserver<VerticalIntResponse> responseObserver) {
    VerticalIntResponse response = null;
    // Check if in the Redis Cache
    response = CacheReadService.getTotalVerticalOfSkierFromCache(request.getResortID(), request.getSeasonID(),
        request.getDayID(), request.getSkierID());
    if (response == null) { // if Not in Redis, get from the DB and then write to Redis
      try {
        response = liftRideReader.getSkierDayVertical(request.getResortID(), request.getSeasonID(),
            request.getDayID(), request.getSkierID());
        // write to Redis
        CacheWriteService.writeVerticalToCache(request.getResortID(),
            request.getSeasonID(),
            request.getDayID(),
            request.getSkierID(),
            response.getTotalVertical());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getResortDaySkiers(ResortDayRequest request,
      StreamObserver<SkierCountResponse> responseObserver) {
    SkierCountResponse response = null;
    // Check if in the Redis Cache
    response = CacheReadService.getUniqueSkierCountFromCache(request.getResortID(),
        request.getSeasonID(),
        request.getDayID());
    if (response == null) { // if Not in Redis, get from the DB and then write to Redis
      try {
        response = liftRideReader.getResortUniqueSkiers(request.getResortID(),
            request.getSeasonID(),
            request.getDayID());
        // write to Redis
        CacheWriteService.writeUniqueSkierCountToCache(
            request.getResortID(),
            request.getSeasonID(),
            request.getDayID(),
            response.getSkierCount());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}