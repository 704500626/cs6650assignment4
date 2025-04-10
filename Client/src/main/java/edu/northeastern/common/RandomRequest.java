package edu.northeastern.common;

import edu.northeastern.utils.RandomUtils;
import edu.northeastern.model.LiftRide;

import java.net.URI;
import java.net.http.HttpRequest;

public class RandomRequest {
    public static final RandomRequest POISON_PILL = new RandomRequest(true); // Special termination signal

    LiftRide liftRide;
    int skierID;
    String seasonID;
    String dayID;
    int resortID;
    boolean isPoisonPill;

    public RandomRequest() {
        this.isPoisonPill = false;
        liftRide = generateRandomLiftRide();
        resortID = RandomUtils.uniform(1, 11);
        seasonID = "2025";
        dayID = "1";
        skierID = RandomUtils.uniform(1, 100001);
    }

    // Constructor for the poison pill
    public RandomRequest(boolean isPoisonPill) {
        this.isPoisonPill = isPoisonPill;
    }

    public static LiftRide generateRandomLiftRide() {
        LiftRide liftRide = new LiftRide();
        liftRide.setLiftID(RandomUtils.uniform(1, 41));
        liftRide.setTime(RandomUtils.uniform(1, 361));
        return liftRide;
    }

    // Helper method to build an HttpRequest for a given RandomRequest.
    public static HttpRequest buildHttpRequestForRandomRequest(RandomRequest request, String serverUrl, String jsonBody) {
        String url = serverUrl + "/skiers/" + request.resortID + "/seasons/" + request.seasonID + "/days/" + request.dayID + "/skiers/" + request.skierID;
        return HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(jsonBody)).header("Content-Type", "application/json").build();
    }

    public LiftRide getLiftRide() {
        return liftRide;
    }

    public void setLiftRide(LiftRide liftRide) {
        this.liftRide = liftRide;
    }

    public int getSkierID() {
        return skierID;
    }

    public void setSkierID(int skierID) {
        this.skierID = skierID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public String getDayID() {
        return dayID;
    }

    public void setDayID(String dayID) {
        this.dayID = dayID;
    }

    public int getResortID() {
        return resortID;
    }

    public void setResortID(int resortID) {
        this.resortID = resortID;
    }

    public boolean isPoisonPill() {
        return isPoisonPill;
    }

    public void setPoisonPill(boolean poisonPill) {
        isPoisonPill = poisonPill;
    }
}
