package gov.bnl.shift;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 *  JDBC query to retrieve shifts from the directory .
 *
 * @author eschuhmacher
 */
public class FindShiftsQuery {

    private Multimap<String, String> value_matches = ArrayListMultimap.create();
    private List<String> shift_ids = new LinkedList<String>();
    private List<String> shift_owners = new LinkedList<String>();
    private List<String> shift_start_dates = new LinkedList<String>();
    private List<String> shift_end_dates = new LinkedList<String>();
    private boolean endDateIsEmpty = false;
    private PreparedStatement ps;

    /**
     * Creates a new instance of FindShiftsQuery, sorting the query parameters.
     * Property matches and tag string matches go to the first inner query,
     * tag pattern matches are queried separately,
     * name matches go to the outer query.
     * Property and tag names are converted to lowercase before being matched.
     *
     * @param matches  the map of matches to apply
     */
    private FindShiftsQuery(final MultivaluedMap<String, String> matches) {
        for (final Map.Entry<String, List<String>> match : matches.entrySet()) {
            final String key = match.getKey().toLowerCase();
            if (key.equals("id")) {
                shift_ids.addAll(match.getValue());
            } else if(key.equals("from")) {
                shift_start_dates.addAll(match.getValue());
            } else if (key.equals("to")) {
                shift_end_dates.addAll(match.getValue());
            } else if (key.equals("owner")) {
                shift_owners.addAll(match.getValue());
            } else {
                value_matches.putAll(key, match.getValue());
            }
        }
    }

    private FindShiftsQuery(final boolean endDateIsEmpty) {
        this.endDateIsEmpty = endDateIsEmpty;
    }

    private FindShiftsQuery(final String shiftId) {
        shift_ids.add(shiftId);
    }

    /**
     * Creates and executes a JDBC based query using subqueries for
     * property and tag matches.
     *
     * @param con  connection to use
     * @return result set with columns named <tt>shift</tt>
     * @throws ShiftFinderException wrapping an SQLException
     */
    private ResultSet executeQuery(final Connection con) throws ShiftFinderException {
        StringBuilder query = new StringBuilder();
        List<String> id_params = new LinkedList<String>();       // parameter lists for the outer query
        List<String> name_params = new LinkedList<String>();
        //no paramethers where pass, in this case return the most recently created shift
        if(shift_owners.isEmpty() && shift_end_dates.isEmpty() && shift_start_dates.isEmpty() && shift_ids.isEmpty() && !endDateIsEmpty) {
            query.append("SELECT * FROM shift ORDER BY start_date DESC limit 1");
        } else {
            boolean used = false;
            query.append("SELECT * FROM shift WHERE ");
            if (!shift_ids.isEmpty()) {
                used = true;
                query.append("id IN (");
                for (String i : shift_ids) {
                    query.append("?,");
                    id_params.add(i);
                }
                query.replace(query.length() - 1, query.length(), ")");

            }
            if (!shift_owners.isEmpty()) {
                if(used) {
                    query.append(" AND ");
                }
                query.append(" owner IN (");
                used = true;
                for (String i : shift_owners) {
                    query.append("?,");
                    name_params.add(i);
                }
                query.replace(query.length() - 1, query.length(), ")");
            }
            if (!shift_start_dates.isEmpty()) {
                if(used) {
                    query.append(" AND ");
                }
                query.append(" start_date >= ");
                used = true;
                query.append("? ");
                name_params.add(shift_start_dates.get(0));
            }
            if (!shift_end_dates.isEmpty()) {
                if(used) {
                    query.append(" AND ");
                }
                query.append(" start_date <= ");
                used = true;
                query.append("? ");
                name_params.add(shift_end_dates.get(0));
            }
            if(endDateIsEmpty) {
                if(used) {
                    query.append(" AND ");
                }
                query.append(" end_date is null");
            }

            query.append(" ORDER BY start_date DESC");
        }
        try {
            ps = con.prepareStatement(query.toString());
            int i = 1;
            for (String p : id_params) {
                ps.setString(i++, p);
            }
            for (String s : name_params) {
                ps.setString(i++, s);
            }

            return ps.executeQuery();
        } catch (SQLException e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "SQL Exception in shift query", e);
        }
    }

    /* Regexp for this pattern: "((\\\\)*)((\\\*)|(\*)|(\\\?)|(\?)|(%)|(_))"
     * i.e. any number of "\\" (group 1) -> same number of "\\"
     * then any of        "\*" (group 4) -> "*"
     *                    "*"  (group 5) -> "%"
     *                    "\?" (group 6) -> "?"
     *                    "?"  (group 7) -> "_"
     *                    "%"  (group 8) -> "\%"
     *                    "_"  (group 9) -> "\_"
     */
    private static Pattern pat = Pattern.compile("((\\\\\\\\)*)((\\\\\\*)|(\\*)|(\\\\\\?)|(\\?)|(%)|(_))");
    private static final int grp[] = {4, 5, 6, 7, 8, 9};
    private static final String rpl[] = {"*", "%", "?", "_", "\\%", "\\_"};

    /**
     * Translates the specified file glob pattern <tt>in</tt>
     * into the corresponding SQL pattern.
     *
     * @param in  file glob pattern
     * @return  SQL pattern
     */
    private static String convertFileGlobToSQLPattern(final String in) {
        StringBuffer out = new StringBuffer();
        Matcher m = pat.matcher(in);

        while (m.find()) {
            StringBuffer rep = new StringBuffer();
            if (m.group(1) != null) {
                rep.append(m.group(1));
            }
            for (int i = 0; i < grp.length; i++) {
                if (m.group(grp[i]) != null) {
                    rep.append(rpl[i]);
                    break;
                }
            }
            m.appendReplacement(out, rep.toString());
        }
        m.appendTail(out);
        return out.toString();
    }


    /**
     * Close the query and release all resources related to it.
     *
     * @throws ShiftFinderException wrapping an SQLException
     */
    private void close() throws ShiftFinderException {
        if (ps != null)
            try {
                ps.close();
            } catch (SQLException e) {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                        "SQL Exception closing shifts query", e);
            }
    }

    /**
     * Finds shifts by matching startDate and/or endDAte.
     *
     * @param matches MultiMap of query parameters
     * @return XMLShifts container with all found shifts
     */
    public static XMLShifts findShiftsByMultiMatch(final MultivaluedMap<String, String> matches) throws ShiftFinderException {
        FindShiftsQuery q = new FindShiftsQuery(matches);
        XMLShifts xmlShifts = new XMLShifts();
        XMLShift xmlShift = null;
        try {
                ResultSet rs = q.executeQuery(DbConnection.getInstance().getConnection());

            Long lastShift = null;
            if (!rs.equals(null)) {
                while (rs.next()) {
                    final Long thisShift = rs.getLong("id");
                    if (!thisShift.equals(lastShift) || rs.isFirst()) {
                        xmlShift = new XMLShift(thisShift, rs.getString("owner"), rs.getDate("start_date"), rs.getDate("end_date"));
                        xmlShifts.addXMLShift(xmlShift);
                        lastShift = thisShift;
                    }
                }
                rs.close();
            }
            q.close();
            return xmlShifts;
        } catch (SQLException e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "SQL Exception while parsing result of find shifts request", e);
        }
    }


    /**
     * Return single shift found by shift id.
     *
     *
     * @param shiftId id to look for
     * @return XMLShift with found shift
     * @throws ShiftFinderException on SQLException
     */
    public static XMLShift findShiftById(final String shiftId) throws ShiftFinderException {
        FindShiftsQuery q = new FindShiftsQuery(shiftId);
        XMLShift xmlShift = null;
        try {
            ResultSet rs = q.executeQuery(DbConnection.getInstance().getConnection());
            if (rs != null) {
                while (rs.next()) {
                    String thisShift = rs.getString("id");
                    if (rs.isFirst()) {
                        xmlShift = new XMLShift(Long.parseLong(thisShift), rs.getString("owner"), rs.getDate("start_date"), rs.getDate("end_date"));
                    }
                }
                rs.close();
            }
            q.close();
            return xmlShift;
        } catch (SQLException e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "SQL Exception while parsing result of single shift search request", e);
        }
    }

    public static XMLShift getOpenShift() throws  ShiftFinderException {
        FindShiftsQuery q = new FindShiftsQuery(true);
        XMLShift xmlShift = null;
        try {
            ResultSet rs = q.executeQuery(DbConnection.getInstance().getConnection());
                if (rs != null) {
                while (rs.next()) {
                    String thisShift = rs.getString("id");
                    if (rs.isFirst()) {
                        xmlShift = new XMLShift(Long.parseLong(thisShift), rs.getString("owner"), rs.getDate("start_date"), rs.getDate("end_date"));
                    }
                }
                rs.close();
            }
            q.close();
            return xmlShift;
        } catch (SQLException e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "SQL Exception while parsing result of single shift search request", e);
        }
    }
}
