package readservice;

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

  private LiftRideReader liftRideReader = new LiftRideReader(config);

  @Override
  public void getTotalVertical(VerticalRequest request, StreamObserver<VerticalListResponse> responseObserver) {
    VerticalRecord record = VerticalRecord.newBuilder()
        .setSeasonID("2025")
        .setTotalVertical(12000)
        .build();

    VerticalListResponse response = VerticalListResponse.newBuilder()
        .addRecords(record)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getSkierDayRides(SkierDayRequest request, StreamObserver<VerticalIntResponse> responseObserver) {
    VerticalIntResponse response = null;
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
    SkierCountResponse response = SkierCountResponse.newBuilder()
        .setSkierCount(120)
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}