package model;

public class GetSkiersVerticalRequest {
    /**
     * GET/skiers/{skierID}/vertical
     * get the total vertical for the skier for specified seasons at the specified resort
     */
    public int skierID;
    public String resortID;
    public String seasonID;

    public GetSkiersVerticalRequest(int skierID, String resortID, String seasonID) {

        this.skierID = skierID;
        this.resortID = resortID;
        this.seasonID = seasonID;
    }


}
