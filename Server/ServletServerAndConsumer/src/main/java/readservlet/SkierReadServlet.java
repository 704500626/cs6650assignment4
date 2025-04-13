package readservlet;

import com.google.gson.Gson;
import model.Configuration;
import model.LiftRideEventMsg;
import model.ResponseMsg;
import ratelimiter.RateLimiter;
import ratelimiter.TokenBucketRateLimiter;
import utils.ConfigUtils;
import writeservlet.RabbitMQPublisher;
import writeservlet.RequestValidator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class SkierReadServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private Configuration config;

    @Override
    public void init() throws ServletException {
        config = ConfigUtils.getConfigurationForServlet(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo(); // "/7/seasons/2025/days/1/skiers/96541"
        if (!RequestValidator.isUrlValid(urlPath)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Invalid inputs: invalid URL or parameters");
            return;
        }
        try {
            String[] urlParts = urlPath.split("/");
            int resortID = Integer.parseInt(urlParts[1]);
            String seasonID = urlParts[3];
            int dayID = Integer.parseInt(urlParts[5]);
            int skierID = Integer.parseInt(urlParts[7]);
            if (!RequestValidator.validateParameters(resortID, seasonID, dayID, skierID)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("Invalid inputs: invalid parameter values");
            }
        } catch (NumberFormatException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Invalid inputs: invalid parameter values");
        }
    }

    @Override
    public void destroy() {
        System.out.println("Shutting down gracefully...");
    }
}