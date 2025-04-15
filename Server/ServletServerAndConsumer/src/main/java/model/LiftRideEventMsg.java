package model;

public class LiftRideEventMsg {
    private int resortID;
    private String seasonID;
    private int dayID;
    private int skierID;
    private LiftRide liftRide;

    public LiftRideEventMsg(int resortID, String seasonID, int dayID, int skierID, LiftRide liftRide) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
        this.liftRide = liftRide;
    }

    public int getResortID() { return resortID; }
    public String getSeasonID() { return seasonID; }
    public int getDayID() { return dayID; }
    public int getSkierID() { return skierID; }
    public LiftRide getLiftRide() { return liftRide; }

    public void setResortID(int resortID) {
        this.resortID = resortID;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public void setDayID(int dayID) {
        this.dayID = dayID;
    }

    public void setSkierID(int skierID) {
        this.skierID = skierID;
    }

    public void setLiftRide(LiftRide liftRide) {
        this.liftRide = liftRide;
    }
}
