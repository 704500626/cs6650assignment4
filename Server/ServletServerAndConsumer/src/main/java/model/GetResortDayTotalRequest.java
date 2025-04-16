package model;

public class GetResortDayTotalRequest {
    /**
     * GET/resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
     * get number of unique skiers at resort/season/day
     */

    public int resortID;
    public int seasonID;
    public int dayID;
    public GetResortDayTotalRequest(int resortID, int seasonID, int dayID) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
    }
}
