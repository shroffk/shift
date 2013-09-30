package gov.bnl.shift;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import javax.ws.rs.core.Response;

/**
 * JDBC query to create one channel.
 *
 * @author eschuhmacher
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
        final StringBuilder query = new StringBuilder("INSERT INTO shift (id, owner, start_date) VALUE (?, ?, ?)");
        try {
            ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, shift.getId());
            ps.setString(2, shift.getOwner());
            ps.setTimestamp(3, new java.sql.Timestamp(shift.getStartDate().getTime()));
            ps.execute();
            final ResultSet rs = ps.getGeneratedKeys();
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
        shift.setStartDate(new Date());
        final CreateShiftQuery q = new CreateShiftQuery(shift);
        q.executeQuery(DbConnection.getInstance().getConnection());
        return shift;
    }
}
