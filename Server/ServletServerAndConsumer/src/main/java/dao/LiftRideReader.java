package dao;

import model.Configuration;
import model.SkierVertical;

import java.sql.SQLException;

public class LiftRideReader {

    public LiftRideReader(Configuration config) {
        // TODO
    }

    public int getResortUniqueSkiers(int resortId, String seasonId, int dayId) throws SQLException {
        return 0;
    }

    public int getSkierDayVertical(int resortId, String seasonId, int dayId, int skierId) throws SQLException {
        return 0;
    }

    public SkierVertical getSkierResortTotals(int skierId, int resortId, String seasonId) throws SQLException {
        return null;
    }
}
