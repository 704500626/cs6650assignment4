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
import skierread.SkierReadServiceOuterClass.VerticalRecord;
import skierread.SkierReadServiceOuterClass.VerticalRequest;
import utils.ConfigUtils;

public class SkierReadServiceImpl extends SkierReadServiceGrpc.SkierReadServiceImplBase {
  private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();
//  private static final CacheReadService cacheReadService = new CacheReadService();
//  private static final CacheWriteService cacheWriteService = new CacheWriteService();

  private LiftRideReader liftRideReader = new LiftRideReader(config);

  @Override
  public void getTotalVertical(VerticalRequest request, StreamObserver<VerticalListResponse> responseObserver) {
    VerticalListResponse response = null;
    if (request.getSeasonID().isEmpty()) {
      try {
        response = liftRideReader.getSkierResortTotals(request.getSkierID(), request.getResortID());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    } else{
      try {
        response = liftRideReader.getSkierResortTotals(request.getSkierID(), request.getResortID(), request.getSeasonID());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getSkierDayRides(SkierDayRequest request, StreamObserver<VerticalIntResponse> responseObserver) {
    VerticalIntResponse response = null;
//     response = cacheReadService.getUniqueSkierCount(request.getResortID(), request.getSeasonID(),
//         request.getDayID(), request.getSkierID());
    try {
      response = liftRideReader.getSkierDayVertical(request.getResortID(), request.getSeasonID(),
          request.getDayID(), request.getSkierID());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getResortDaySkiers(ResortDayRequest request, StreamObserver<SkierCountResponse> responseObserver) {
    SkierCountResponse response = null;
    try {
      response = liftRideReader.getResortUniqueSkiers(request.getResortID(), request.getSeasonID(), request.getDayID());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}