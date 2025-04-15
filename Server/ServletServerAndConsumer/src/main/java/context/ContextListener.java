package context;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import model.Configuration;
import ratelimiter.RateLimiter;
import ratelimiter.RateLimiterFactory;
import utils.ConfigUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
// import javax.servlet.annotation.WebListener;

// TODO refactor Configuration of all servlet into context
// Using web.xml for all configurations
// @WebListener
public class ContextListener implements ServletContextListener {
    private ManagedChannel channel;
    private RateLimiter rateLimiter;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        Configuration config = null;
        try {
            config = ConfigUtils.getConfigurationForServlet(ctx);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        rateLimiter = RateLimiterFactory.create(config.RATE_LIMIT_READ_MODE, config.RATE_LIMITER_SERVICE_HOST, config.RATE_LIMITER_READ_SERVLET_GROUP_ID, config.RATE_LIMITER_SERVICE_PORT, config.RATE_LIMITER_READ_MAX_TOKENS, config.RATE_LIMITER_READ_REFILL_RATE);
        channel = ManagedChannelBuilder
                .forAddress(config.LIFTRIDE_READ_SERVICE_HOST, config.LIFTRIDE_READ_SERVICE_PORT)
                .usePlaintext()
                .build();
        ctx.setAttribute("grpcChannel", channel);
        ctx.setAttribute("rateLimiter", rateLimiter);
        ctx.setAttribute("Configuration", config); // optional: reuse in servlets
        System.out.println("[gRPC] Channel created for host=" + config.LIFTRIDE_READ_SERVICE_HOST +
                ", port=" + config.LIFTRIDE_READ_SERVICE_PORT);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            System.out.println("[gRPC] Channel shut down gracefully.");
        }
    }
}



