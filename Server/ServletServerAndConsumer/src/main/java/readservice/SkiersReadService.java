package readservice;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.*;
import model.Configuration;
import utils.ConfigUtils;

public class SkiersReadService {
    private static final Configuration config = ConfigUtils.getConfigurationForLiftRideService();

    public static void main(String[] args) throws Exception {
    }
}
