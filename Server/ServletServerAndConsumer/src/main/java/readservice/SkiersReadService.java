package readservice;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Configuration;
import utils.ConfigUtils;

public class SkiersReadService {
    private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50051)
            .addService(new SkierReadServiceImpl())
            .build()
            .start();

        System.out.println("✅ SkierReadService gRPC server started on port 50051");
        server.awaitTermination();
    }
}
