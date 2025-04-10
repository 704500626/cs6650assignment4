package servlet;

import com.google.gson.Gson;
import model.LiftRide;
import model.LiftRideEventMsg;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestValidator {
    private static final Gson gson = new Gson();

    public static boolean isUrlValid(String urlPath) {
        if (urlPath == null || urlPath.isEmpty()) return false;
        String[] parts = urlPath.split("/");
        return parts.length == 8 && "seasons".equals(parts[2])
                && "days".equals(parts[4]) && "skiers".equals(parts[6]);
    }

    public static LiftRideEventMsg parseAndValidate(HttpServletRequest request, String urlPath) throws IOException {
        String[] urlParts = urlPath.split("/");
        int resortID = Integer.parseInt(urlParts[1]);
        String seasonID = urlParts[3];
        int dayID = Integer.parseInt(urlParts[5]);
        int skierID = Integer.parseInt(urlParts[7]);

        if (!validateParameters(resortID, seasonID, dayID, skierID)) return null;
        LiftRide liftRide = gson.fromJson(request.getReader(), LiftRide.class);
        if (!validateLiftRide(liftRide)) return null;
        return new LiftRideEventMsg(resortID, seasonID, dayID, skierID, liftRide);
    }

    private static boolean validateLiftRide(LiftRide liftRide) {
        return liftRide != null &&
                liftRide.getLiftID() >= 1 && liftRide.getLiftID() <= 40 &&
                liftRide.getTime() >= 1 && liftRide.getTime() <= 360;
    }

    public static boolean validateParameters(int resortID, String seasonID, int dayID, int skierID) {
        return skierID >= 1 && skierID <= 100000 &&
                resortID >= 1 && resortID <= 10 &&
                "2025".equals(seasonID) &&
                dayID == 1;
    }
}