package writeservlet;

import com.google.gson.Gson;
import model.Configuration;
import model.LiftRideEventMsg;
import model.ResponseMsg;
import ratelimiter.RateLimiter;
import ratelimiter.RateLimiterFactory;
import ratelimiter.algorithms.TokenBucketRateLimiter;
import utils.ConfigUtils;
import utils.RequestValidator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class SkierWriteServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private Configuration config;
    private RateLimiter rateLimiter;
    private RabbitMQPublisher publisher;

    @Override
    public void init() throws ServletException {
        config = ConfigUtils.getConfigurationForServlet(getServletContext());
        rateLimiter = RateLimiterFactory.create(config.RATE_LIMIT_WRITE_MODE, config.RATE_LIMITER_SERVICE_HOST, config.RATE_LIMITER_READ_SERVLET_GROUP_ID, config.RATE_LIMITER_SERVICE_PORT, config.RATE_LIMITER_WRITE_MAX_TOKENS, config.RATE_LIMITER_WRITE_REFILL_RATE);
        try {
            publisher = new RabbitMQPublisher(config);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize RabbitMQ connection", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String urlPath = request.getPathInfo();

        // Validate URL and parameters
        if (!RequestValidator.isUrlValid(urlPath)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ResponseMsg("Invalid inputs: invalid URL or parameters")));
            return;
        }

        // Check circuit breaker status via publisher
        if (publisher.isCircuitOpen()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.write(gson.toJson(new ResponseMsg("Circuit open: Server is overloaded. Try again later.")));
            return;
        }

        // Apply rate limiting with retries
        if (config.RATE_LIMITER_WRITE_SWITCH && !rateLimiter.allowRequestWithRetries(config.MAX_RETRIES, config.RATE_LIMITER_MAX_BACKOFF_MS)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.write(gson.toJson(new ResponseMsg("Rate limit exceeded. Please try again later.")));
            return;
        }

        // Wait until the queue depth is acceptable
        if (!publisher.waitForAcceptableQueueDepth(config.MAX_RETRIES, config.RATE_LIMITER_MAX_BACKOFF_MS)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            out.write(gson.toJson(new ResponseMsg("Queue overload, please try again later.")));
            return;
        }

        // Process request and publish event
        try {
            LiftRideEventMsg liftRideMsg = RequestValidator.parseAndValidate(request, urlPath);
            if (liftRideMsg == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(gson.toJson(new ResponseMsg("Invalid inputs: invalid parameter values or LiftRide values")));
                return;
            }
            if (publisher.publishLiftRideEvent(liftRideMsg)) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.write(gson.toJson(new ResponseMsg("Write successful: the liftRideMsg has been created")));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(gson.toJson(new ResponseMsg("Failed to send message to RabbitMQ")));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ResponseMsg("Invalid inputs: malformed JSON in request body")));
        }
    }

    @Override
    public void destroy() {
        publisher.shutdown();
        System.out.println("Shutting down gracefully...");
    }
}