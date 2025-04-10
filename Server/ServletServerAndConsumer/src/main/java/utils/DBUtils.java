package utils;

import model.LiftRideEventMsg;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBUtils {
    /**
     * Helper method to set parameters on the PreparedStatement for a given event.
     */
    public static void setParametersForEvent(PreparedStatement stmt, LiftRideEventMsg event) throws SQLException {
        stmt.setInt(1, event.getSkierID());
        stmt.setInt(2, event.getResortID());
        stmt.setString(3, event.getSeasonID());
        stmt.setInt(4, event.getDayID());
        stmt.setInt(5, event.getLiftRide().getLiftID());
        stmt.setInt(6, event.getLiftRide().getTime());
    }
}
