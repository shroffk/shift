package gov.bnl.shift;

import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author eschuhmacher
 */
public class ShiftManager {

    private static ShiftManager instance = new ShiftManager();

    /**
     * Create an instance of ShiftManager
     */
    private ShiftManager() {
    }

    /**
     * Returns the (singleton) instance of ShiftManager
     *
     * @return the instance of ShiftManager
     */
    public static ShiftManager getInstance() {
        return instance;
    }

    /**
     * Return single shift found by shift id.
     *
     *
     * @param shiftId id to look for
     * @return XMLShift with found shift
     * @throws ShiftFinderException on SQLException
     */
    public XMLShift findShiftById(final String shiftId) throws ShiftFinderException {
        return FindShiftsQuery.findShiftById(shiftId);
    }


    /**
     * Returns multiple shifts found by matching ids, start dates or owner name.
     *
     * @param matches multivalued map of patterns to match
     * their values against.
     * @return XMLShifts container
     * @throws ShiftFinderException wrapping an SQLException
     */
    public XMLShifts findShiftsByMultiMatch(final MultivaluedMap<String, String> matches) throws ShiftFinderException {
        return FindShiftsQuery.findShiftsByMultiMatch(matches);
    }


    /**
     * Add the end Date to a shift
     * specified in the XMLShift <tt>shift</tt>.
     *
     *
     * @param shift XMLShift shift to end
     * @throws ShiftFinderException on ownership mismatch, or wrapping an SQLException
     */
    public XMLShift endShift(final XMLShift shift) throws ShiftFinderException {
        return UpdateShiftQuery.updateEndDateShift(shift);
    }

    /**
     * Find the last open shift
     * @throws ShiftFinderException
     */
    public XMLShift getOpenShift() throws ShiftFinderException {
        return FindShiftsQuery.getOpenShift();
    }

    /**
     * Create a new Shift instance
     * @param shift XMLShift
     * @throws ShiftFinderException
     */
    public XMLShift startShift(final XMLShift shift) throws ShiftFinderException {
        return CreateShiftQuery.createShift(shift);
    }

}
