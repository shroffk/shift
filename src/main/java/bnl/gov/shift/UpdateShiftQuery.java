package bnl.gov.shift;

import java.sql.*;
import javax.ws.rs.core.Response;

/**
 * JDBC query to add a propertys to shift(s).
 *
 * @author eschuhmacher
 */
public class UpdateShiftQuery {

    private XMLShift shift;

    private UpdateShiftQuery(final XMLShift xmlShift) {
        this.shift = xmlShift;
    }

    public void executeQuery(final Connection con) throws ShiftFinderException {
        PreparedStatement ps;
        // update shift
        final StringBuilder query = new StringBuilder("UPDATE shift set end_date = ? WHERE id = ?");
        try {
            ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, shift.getId());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            rs.first();
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "SQL Exception while ending shift '" + shift.getId() +"'", e);
        }
    }


    /**
     * Updates the <tt>endDate</tt> in the database, adding it to the shift.
     *
     * @param shift XMLShift
     * @throws ShiftFinderException wrapping an SQLException
     */
    public static void updateEndDateShift(XMLShift shift) throws ShiftFinderException {
        UpdateShiftQuery q = new UpdateShiftQuery(shift);
        q.executeQuery(DbConnection.getInstance().getConnection());
    }
}