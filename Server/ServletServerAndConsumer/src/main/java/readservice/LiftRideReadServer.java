package readservice;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Configuration;
import utils.ConfigUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LiftRideReadServer {
    public static void main(String[] args) throws Exception {
        Configuration config = ConfigUtils.getConfigurationForService();

        ExecutorService executor = new ThreadPoolExecutor(
                config.LIFTRIDE_READ_SERVICE_MIN_THREAD,          // core pool size
                config.LIFTRIDE_READ_SERVICE_MAX_THREAD,                     // max pool size
                60, TimeUnit.SECONDS,    // idle timeout
                new LinkedBlockingQueue<>(config.LIFTRIDE_READ_SERVICE_REQUEST_QUEUE_SIZE)  // request queue size (like acceptCount)
        );

        LiftRideReadServiceImpl liftRideReadService = new LiftRideReadServiceImpl(config);
        Server server = ServerBuilder.forPort(config.LIFTRIDE_READ_SERVICE_PORT)
                .executor(executor)
                .addService(liftRideReadService)
                .build()
                .start();
        System.out.println("LiftRide Read Service gRPC server started on port " + config.LIFTRIDE_READ_SERVICE_PORT);
        server.awaitTermination();

        Runtime.getRuntime().addShutdownHook(new Thread(liftRideReadService::close));
    }
}
