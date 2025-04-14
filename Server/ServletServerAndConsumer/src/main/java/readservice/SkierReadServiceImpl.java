package readservice;

import static java.util.Objects.*;

import cacheservice.CacheReadService;
import cacheservice.CacheWriteService;
import dao.LiftRideReader;
import io.grpc.stub.StreamObserver;
import java.sql.SQLException;
import java.util.Objects;
import model.Configuration;
import skierread.SkierReadServiceGrpc;
import skierread.SkierReadServiceOuterClass.ResortDayRequest;
import skierread.SkierReadServiceOuterClass.SkierCountResponse;
import skierread.SkierReadServiceOuterClass.SkierDayRequest;
import skierread.SkierReadServiceOuterClass.VerticalIntResponse;
import skierread.SkierReadServiceOuterClass.VerticalListResponse;
import skierread.SkierReadServiceOuterClass.VerticalRecord;
import skierread.SkierReadServiceOuterClass.VerticalRequest;
import utils.ConfigUtils;

public class SkierReadServiceImpl extends SkierReadServiceGrpc.SkierReadServiceImplBase {

  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

  private LiftRideReader liftRideReader = new LiftRideReader(config);

  @Override
  public void getTotalVertical(VerticalRequest request,
      StreamObserver<VerticalListResponse> responseObserver) {
    VerticalListResponse response = null;
    // Check if in the Redis Cache
    response = CacheReadService.getTotalVertical(request.getResortID(), request.getSeasonID(),
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
      CacheWriteService.writeVerticalList(request.getSkierID(),
          request.getResortID(),
          request.getSeasonID(),
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
    response = CacheReadService.getUniqueSkierCount(request.getResortID(), request.getSeasonID(),
        request.getDayID(), request.getSkierID());
    if (response == null) { // if Not in Redis, get from the DB and then write to Redis
      try {
        response = liftRideReader.getSkierDayVertical(request.getResortID(), request.getSeasonID(),
            request.getDayID(), request.getSkierID());
        // write to Redis
        CacheWriteService.writeVertical(request.getResortID(),
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
        CacheWriteService.writeUniqueSkierCount(
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