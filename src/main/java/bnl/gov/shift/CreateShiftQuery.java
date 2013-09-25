package bnl.gov.shift;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.ws.rs.core.Response;

/**
 * JDBC query to create one channel.
 *
 * @author Ralph Lange <Ralph.Lange@helmholtz-berlin.de>
 */
public class CreateShiftQuery {

    private XMLShift shift;

    private CreateShiftQuery(XMLShift shift) {
        this.shift = shift;
    }

    /**
     * Executes a JDBC based query to add a shift
     *
     * @param con database connection to use
     * @throws ShiftFinderException wrapping an SQLException
     */
    private void executeQuery(final Connection con) throws ShiftFinderException {
        final PreparedStatement ps;
        // Insert shift
        //TODO: check if any shift do not have a end date, if so please return the open shift
        StringBuilder query = new StringBuilder("INSERT INTO shift (id, owner, start_date) VALUE (?, ?, ?)");
        try {
            ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, shift.getId());
            ps.setString(2, shift.getOwner());
            ps.setLong(3, System.currentTimeMillis());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            rs.first();
            rs.close();
            ps.close();
        } catch (SQLException e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "SQL Exception while adding shift '" + shift.getId() +"'", e);
        }
    }

    /**
     * Creates a shift in the database.
     *
     * @param shift XMLShift object
     * @throws ShiftFinderException wrapping an SQLException
     */
    public static XMLShift createShift(final XMLShift shift) throws ShiftFinderException {
        final CreateShiftQuery q = new CreateShiftQuery(shift);
        q.executeQuery(DbConnection.getInstance().getConnection());
        return shift;
    }
}
