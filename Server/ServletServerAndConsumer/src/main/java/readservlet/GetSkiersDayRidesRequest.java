package readservlet;

public class GetSkiersDayRidesRequest {
    /**
     * GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
     * get the total vertical for the skier for the specified ski day
     */

    public int resortID;
    public int seasonID;
    public int dayID;
    public int skierID;

    public GetSkiersDayRidesRequest(int resortID, int seasonID, int dayID, int skierID) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
    }
}
