package readservlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import model.Configuration;
import skierread.SkierReadServiceGrpc;
import skierread.SkierReadServiceOuterClass;
import utils.ConfigUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SkierReadServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private Configuration config;
    private GetRequestValidator getRequestValidator;

    @Override
    public void init() throws ServletException {
        config = ConfigUtils.getConfigurationForServlet(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo(); // "/7/seasons/2025/days/1/skiers/96541"
        System.out.println(urlPath);
        if (!GetRequestValidator.isUrlValid(urlPath)) {
            res.setStatus(400);
            res.getWriter().write("{\"message\":\"Invalid path\"}");
            return;
        }

        String[] parts = urlPath.split("/");

        GetRequestValidator.GetRequestType type = GetRequestValidator.classifyGetRequest(parts);

        switch (type) {
            case VERTICAL_LOOKUP:
                GetSkiersVerticalRequest verticalReq = GetRequestValidator.parseSkierDayRequest(req, parts);
                System.out.println(verticalReq.skierID + " " + verticalReq.resortID + " " + verticalReq.seasonID);
                ManagedChannel verticalChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                        .usePlaintext()
                        .build();
                SkierReadServiceGrpc.SkierReadServiceBlockingStub stub = SkierReadServiceGrpc.newBlockingStub(verticalChannel);
                SkierReadServiceOuterClass.VerticalRequest request = SkierReadServiceOuterClass.VerticalRequest.newBuilder()
                        .setSkierID(verticalReq.skierID)
                        .setResortID(Integer.parseInt(verticalReq.resortID))
                        .setSeasonID(verticalReq.seasonID)
                        .build();

                SkierReadServiceOuterClass.VerticalListResponse response = stub.getTotalVertical(request);
                JsonArray jsonArray = new JsonArray();
                for (SkierReadServiceOuterClass.VerticalRecord record : response.getRecordsList()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("seasonID", record.getSeasonID());
                    obj.addProperty("totalVertical", record.getTotalVertical());
                    jsonArray.add(obj);
                }

                JsonObject finalObj = new JsonObject();
                finalObj.add("records", jsonArray);
                res.setStatus(HttpServletResponse.SC_OK);
                res.setContentType("application/json");
                res.getWriter().write(finalObj.toString());

                verticalChannel.shutdownNow();
                break;

            case SKIERS_DAY_RIDES:
                var ridesReq = GetRequestValidator.parseSkierDayRideRequest(parts);
                ManagedChannel ridesChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                        .usePlaintext()
                        .build();
                try{
                    SkierReadServiceGrpc.SkierReadServiceBlockingStub ridesStub = SkierReadServiceGrpc.newBlockingStub(ridesChannel);
                    SkierReadServiceOuterClass.SkierDayRequest ridesRequest =
                            SkierReadServiceOuterClass.SkierDayRequest.newBuilder()
                                    .setSkierID(ridesReq.skierID)
                                    .setResortID(ridesReq.resortID)
                                    .setSeasonID(String.valueOf(ridesReq.seasonID))
                                    .setDayID(ridesReq.dayID)
                                    .build();

                    SkierReadServiceOuterClass.VerticalIntResponse ridesResp = ridesStub.getSkierDayRides(ridesRequest);

                    JsonObject result = new JsonObject();
                    result.addProperty("totalVertical", ridesResp.getTotalVertical());

                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write(result.toString());
                } finally {
                    ridesChannel.shutdownNow();
                }
                break;


            case RESORT_DAY_TOTAL:
                var resortReq = GetRequestValidator.parseResortDayTotalRequest(parts);

                ManagedChannel resortChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                        .usePlaintext()
                        .build();

                try {
                    SkierReadServiceGrpc.SkierReadServiceBlockingStub resortStub =
                            SkierReadServiceGrpc.newBlockingStub(resortChannel);

                    SkierReadServiceOuterClass.ResortDayRequest resortRequest =
                            SkierReadServiceOuterClass.ResortDayRequest.newBuilder()
                                    .setResortID(resortReq.resortID)
                                    .setSeasonID(String.valueOf(resortReq.seasonID))
                                    .setDayID(resortReq.dayID)
                                    .build();

                    SkierReadServiceOuterClass.SkierCountResponse resortResp =
                            resortStub.getResortDaySkiers(resortRequest);

                    JsonObject result = new JsonObject();
                    result.addProperty("skierCount", resortResp.getSkierCount());

                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write(result.toString());
                } finally {
                    resortChannel.shutdownNow();
                }
            break;

            default:
                res.setStatus(400);
                res.getWriter().write("{\"message\":\"Unknown request type\"}");
        }
    }

    @Override
    public void destroy() {
        System.out.println("Shutting down gracefully...");
    }
}