package gov.bnl.shiftClient;

import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
*@author: eschuhmacher
 */
public interface ShiftClient {

    /**
     * Get a list of all the shifts currently existings
     *
     * @return string collection of shifts
     * @throws ShiftFinderException
     */
    public Collection<Shift> listShifts(final String type) throws ShiftFinderException;


    /**
     * Returns a shift that exactly matches the shiftId <tt>shiftId</tt>
     *
     * @param shiftId shiftId
     * @return Shift object
     * @throws ShiftFinderException
     */
    public Shift getShift(final Integer shiftId, final String type) throws ShiftFinderException;


    /**
     * start a single shift <tt>shift</tt>,
     *
     *
     * @param shift the shift to be started
     * @throws ShiftFinderException
     */
    public Shift start(Shift shift) throws ShiftFinderException;

    /**
     * end a shift
     *
     *
     * @param shift to be close
     * @throws ShiftFinderException
     */
    public Shift end(Shift shift) throws ShiftFinderException;

    /**
     * close a shift
     *
     *
     * @param shift  shift to be ended
     * @throws ShiftFinderException
     */
    public Shift close(Shift shift) throws ShiftFinderException;


    /**
     *
     * @param pattern
     * @return collection of Shift objects
     * @throws ShiftFinderException
     */
    public Collection<Shift> findShiftsBySearch(String pattern) throws ShiftFinderException;

    /**
     * Query for shifts based on the criteria specified in the map
     *
     * @param map
     * @return collection of Shift objects
     * @throws ShiftFinderException
     */
    public Collection<Shift> findShifts(final String type, final Map<String, String> map)  throws ShiftFinderException;

    /**
     * Multivalued map used to search for a key with multiple values. e.g.
     * shift a=1 or shift a=2
     *
     *
     * @param map Multivalue map for searching a key with multiple values
     * @param type
     * @return collection of shift objects
     * @throws ShiftFinderException
     */
    public Collection<Shift> findShifts(MultivaluedMap<String, String> map, String type) throws ShiftFinderException;
}
