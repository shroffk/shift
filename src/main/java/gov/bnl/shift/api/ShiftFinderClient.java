/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package gov.bnl.shift.api;

import javax.ws.rs.core.*;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author eschuhmacher
 *
 */
public interface ShiftFinderClient {

    /**
     * Returns a shift that exactly matches the shift Id
     * <tt>shiftId</tt>.
     *
     * @param shiftId - shift id.
     * @return {@link Shift} with name <tt>shiftId</tt> or null
     * @throws ShiftFinderException
     */
    Shift getShift(final Long shiftId) throws ShiftFinderException;

    /**
     * Destructively set a single shift <tt>shift</tt>, if the shift
     * already exists return error.
     *
     * @param shift - the shift to be added
     * @throws ShiftFinderException
     */
    void set(Shift.Builder shift) throws ShiftFinderException;

    /**
     * Update existing shift with <tt>shift</tt>.
     *
     * @param shift
     * @throws ShiftFinderException
     */
    public void update(Shift.Builder shift) throws ShiftFinderException;

    /**
     * Search for shifts who's id match the pattern <tt>pattern</tt>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern
     *            - the search pattern for the shift Ids
     * @return A Collection of shifts who's name match the pattern
     *         <tt>pattern</tt>
     * @throws ShiftFinderException
     */
    public Collection<Shift> findById(String pattern)
            throws ShiftFinderException;

    /**
     * Search for shifts with tags who's name match the pattern
     * <tt>pattern</tt>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern
     *            - the search pattern for the tag names
     * @return A Collection of shifts which contain tags who's name match the
     *         pattern <tt>pattern</tt>
     * @throws ShiftFinderException
     */
    public Collection<Shift> findByStartDate(String pattern)
            throws ShiftFinderException;


    public Collection<Shift> find(String query) throws ShiftFinderException;

    /**
     * Query for shift based on the multiple criteria specified in the map.
     * Map.put("~id", "*")<br>
     * Map.put("~startDate", "08/12/12")<br>
     *
     * this will return all shifts with id=any name AND startDate=08/12/12
     *
     * @param map
     * @return Collection of shifts which satisfy the search map.
     * @throws ShiftFinderException
     */
    public Collection<Shift> find(Map<String, String> map)
            throws ShiftFinderException;

    /**
     * uery for shifts based on the multiple criteria specified in the map.
     * Map.put("~name", "*")<br>
     * Map.put("~tag", "tag1")<br>
     * Map.put("Cell", "1")<br>
     * Map.put("Cell", "2")<br>
     * Map.put("Cell", "3")<br>
     *
     * this will return all shifts with name=any name AND tag=tag1 AND
     * property Cell = 1 OR 2 OR 3.
     *
     * @param map
     *            - multivalued map of all search criteria
     * @return Collection of shifts which satisfy the search map.
     * @throws ShiftFinderException
     */
    public Collection<Shift> find(MultivaluedMap<String, String> map)
            throws ShiftFinderException;

    /**
     * close
     */
    public void close();

}
