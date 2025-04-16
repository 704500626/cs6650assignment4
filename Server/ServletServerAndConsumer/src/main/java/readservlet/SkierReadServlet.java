package readservlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import grpc.LiftRideReadProto.*;
import grpc.SkierReadServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import model.Configuration;
import model.GetSkiersVerticalRequest;
import model.ResponseMsg;
import ratelimiter.RateLimiter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SkierReadServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private Configuration config;
    private SkierReadServiceGrpc.SkierReadServiceBlockingStub stub;
    private ManagedChannel channel;
    private RateLimiter rateLimiter;

    @Override
    public void init() throws ServletException {
        config = (Configuration) getServletContext().getAttribute("Configuration");
        channel = (ManagedChannel) getServletContext().getAttribute("grpcChannel");
        rateLimiter = (RateLimiter) getServletContext().getAttribute("rateLimiter");
        if (config == null || channel == null)
            throw new ServletException("Configuration or gRPC channel not found in ServletContext");
        stub = SkierReadServiceGrpc.newBlockingStub(channel);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo(); // "/7/seasons/2025/days/1/skiers/96541"
        if (!GetRequestValidator.isUrlValid(urlPath)) {
            System.err.println("Invalid inputs: " + urlPath);
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ResponseMsg("Invalid inputs: Missing path info")));
            return;
        }

        // Apply rate limiting with retries
        if (config.RATE_LIMITER_READ_SWITCH && !rateLimiter.allowRequestWithRetries(config.MAX_RETRIES, config.RATE_LIMITER_MAX_BACKOFF_MS)) {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getWriter().write(gson.toJson(new ResponseMsg("Rate limit exceeded. Please try again later.")));
            return;
        }

        String[] parts = urlPath.split("/");

        GetRequestValidator.GetRequestType type = GetRequestValidator.classifyGetRequest(parts);

        try {
            switch (type) {
                case VERTICAL_LOOKUP: {
                    GetSkiersVerticalRequest verticalReq = GetRequestValidator.parseSkierDayRequest(req, parts);
                    VerticalRequest request = VerticalRequest.newBuilder().setSkierID(verticalReq.skierID).setResortID(Integer.parseInt(verticalReq.resortID)).setSeasonID(verticalReq.seasonID == null ? "" : verticalReq.seasonID).build();
                    VerticalListResponse response = stub.getTotalVertical(request);
                    JsonArray jsonArray = new JsonArray();
                    for (VerticalRecord record : response.getRecordsList()) {
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
                    break;
                }

                case SKIERS_DAY_RIDES: {
                    var ridesReq = GetRequestValidator.parseSkierDayRideRequest(parts);
                    SkierDayRequest ridesRequest = SkierDayRequest.newBuilder().setSkierID(ridesReq.skierID).setResortID(ridesReq.resortID).setSeasonID(String.valueOf(ridesReq.seasonID)).setDayID(ridesReq.dayID).build();
                    VerticalIntResponse ridesResp = stub.getSkierDayRides(ridesRequest);
                    JsonObject result = new JsonObject();
                    result.addProperty("totalVertical", ridesResp.getTotalVertical());
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write(result.toString());
                    break;
                }
                case RESORT_DAY_TOTAL: {
                    var resortReq = GetRequestValidator.parseResortDayTotalRequest(parts);
                    ResortDayRequest resortRequest = ResortDayRequest.newBuilder().setResortID(resortReq.resortID).setSeasonID(String.valueOf(resortReq.seasonID)).setDayID(resortReq.dayID).build();
                    SkierCountResponse resortResp = stub.getResortDaySkiers(resortRequest);
                    JsonObject result = new JsonObject();
                    result.addProperty("skierCount", resortResp.getSkierCount());
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write(result.toString());
                    break;
                }
                default:
                    System.err.println("Unrecognized request type: " + urlPath);
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().write("{\"message\":\"Unknown request type\"}");
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(gson.toJson(new ResponseMsg("Error parsing parameters: " + e.getMessage())));
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            if (e.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED) {
                res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                res.getWriter().write(gson.toJson(new ResponseMsg("Service Unavailable")));
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write(gson.toJson(new ResponseMsg("Unknown Service Error")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write(gson.toJson(new ResponseMsg("Internal Server Error")));
        }
    }

    @Override
    public void destroy() {
        if (channel != null) {
            try {
                channel.shutdown();
                channel.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Shutting down gracefully...");
    }
}