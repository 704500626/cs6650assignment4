package readservlet;

import model.GetResortDayTotalRequest;
import model.GetSkiersDayRidesRequest;
import model.GetSkiersVerticalRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GetRequestValidator {
    private static final Set<Integer> validPartsLength = new HashSet<>(Arrays.asList(4, 8, 9));
    public enum GetRequestType {
        /**
         * GET/skiers/{skierID}/vertical
         * get the total vertical for the skier for specified seasons at the specified resort
         */
        VERTICAL_LOOKUP,

        /**
         * GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
         * get the total vertical for the skier for the specified ski day
         */
        SKIERS_DAY_RIDES,

        /**
         * GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
         * get number of unique skiers at resort/season/day
         */
        RESORT_DAY_TOTAL,
        UNKNOWN
    }

    public static boolean isUrlValid(String url) {
        if (url == null || url.isEmpty()) return false;
        if (url.equals("/")) return false;
        String[] parts = url.split("/");
        return validPartsLength.contains(parts.length);
    }

    public static GetRequestType classifyGetRequest(String[] parts) {
        if (parts == null || parts.length == 0) return GetRequestType.UNKNOWN;
        switch (parts.length) {
            case 4:
                return isValidVertical(parts) ? GetRequestType.VERTICAL_LOOKUP : GetRequestType.UNKNOWN;
            case 8:
                return isValidResortDayVertical(parts) ? GetRequestType.RESORT_DAY_TOTAL : GetRequestType.UNKNOWN;
            case 9:
                return isValidSkierDayRides(parts) ? GetRequestType.SKIERS_DAY_RIDES : GetRequestType.UNKNOWN;

        }

        return GetRequestType.UNKNOWN;
    }

    public static boolean isValidVertical(String[] parts) {
        String stringSkiers = parts[1];
        int skierID = Integer.parseInt(parts[2]);
        String stringVertical = parts[3];

        return stringSkiers.equals("skiers") && stringVertical.equals("vertical") && skierID >= 1 && skierID <= 100000;
    }

    public static boolean isValidSkierDayRides(String[] parts) {
        String stringSkiers = parts[1];
        String stringSeasons = parts[3];
        String stringDays = parts[5];
        String stringSkiers2 = parts[7];

        int resortID = Integer.parseInt(parts[2]);
        int seasonID = Integer.parseInt(parts[4]);
        int dayID = Integer.parseInt(parts[6]);
        int skierID = Integer.parseInt(parts[8]);

        return stringSkiers.equals("skiers") &&
                stringSeasons.equals("seasons") &&
                stringDays.equals("days") &&
                stringSkiers2.equals("skiers") &&
                resortID >= 1 && resortID <= 10 &&
                seasonID >= 1000 && seasonID <= 9999 &&
                dayID >= 1 && dayID <= 3 &&
                skierID >= 1 && skierID <= 100000;
    }

    public static boolean isValidResortDayVertical(String[] parts) {
        // GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
        String stringResort = parts[1];
        String stringSeasons = parts[3];
        String stringDays = parts[5];
        String stringSkiers = parts[7];
        int resortID = Integer.parseInt(parts[2]);
        int seasonID = Integer.parseInt(parts[4]);
        int dayID = Integer.parseInt(parts[6]);

        return stringResort.equals("resorts") &&
                stringSeasons.equals("seasons") &&
                stringDays.equals("day") &&
                stringSkiers.equals("skiers") &&
                resortID >= 1 && resortID <= 10 &&
                seasonID >= 1000 && seasonID <= 9999 &&
                dayID >= 1 && dayID <= 3;
    }


    public static GetSkiersVerticalRequest parseSkierDayRequest(HttpServletRequest req, String[] parts) {
        String resortID = req.getParameter("resortID");
        String seasonID = req.getParameter("seasonID");
        if (resortID == null || resortID.isEmpty()) {
            throw new IllegalArgumentException("Missing required 'resort' parameter");
        }
        return new GetSkiersVerticalRequest(Integer.parseInt(parts[2]), resortID, seasonID);
    }

    public static GetSkiersDayRidesRequest parseSkierDayRideRequest(String[] parts) {
        int resortID = Integer.parseInt(parts[2]);
        int seasonID = Integer.parseInt(parts[4]);
        int dayID = Integer.parseInt(parts[6]);
        int skierID = Integer.parseInt(parts[8]);
        return new GetSkiersDayRidesRequest(resortID, seasonID, dayID, skierID);
    }

    public static GetResortDayTotalRequest parseResortDayTotalRequest(String[] parts) {
        int resortID = Integer.parseInt(parts[2]);
        int seasonID = Integer.parseInt(parts[4]);
        int dayID = Integer.parseInt(parts[6]);
        return new GetResortDayTotalRequest(resortID, seasonID, dayID);
    }
}
