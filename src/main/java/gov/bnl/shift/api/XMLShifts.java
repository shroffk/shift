/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package gov.bnl.shift.api;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

/**
 * shifts (collection) object that can be represented as XML/JSON in payload data.
 *
 * @author eschuhmacher
 */

@XmlRootElement(name = "shifts")
public class XMLShifts {
    private Collection<XMLShift> shifts = new ArrayList<XMLShift>();

    /** Creates a new instance of XMLShifts. */
    public XMLShifts() {
    }

    /** Creates a new instance of XMLShifts with one initial shift.
     * @param s initial element
     */
    public XMLShifts(XMLShift s) {
        shifts.add(s);
    }

    /**
     * Returns a collection of XMLShift.
     *
     * @return a collection of XMLShift
     */
    @XmlElement(name = "shift")
    public Collection<XMLShift> getShifts() {
        return shifts;
    }

    /**
     * Sets the collection of shifts.
     *
     * @param items new shift collection
     */
    public void setShifts(Collection<XMLShift> items) {
        this.shifts = items;
    }

    /**
     * Adds a shift to the shift collection.
     *
     * @param item the XMLShift to add
     */
    public void addXMLShift(XMLShift item) {
        this.shifts.add(item);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XMLShift to create the string representation for
     * @return string representation
     */
    public static String toLog(XMLShifts data) {
        if (data.getShifts().isEmpty()) {
            return "[None]";
        } else {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (XMLShift c : data.getShifts()) {
                s.append(XMLShift.toLog(c) + ",");
            }
            s.delete(s.length()-1, s.length());
            s.append("]");
            return s.toString();
        }
    }
}