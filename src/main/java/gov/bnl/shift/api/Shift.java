/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package gov.bnl.shift.api;

import java.util.*;


/**
 * A Channel object represents channel finder channel.<br>
 * Each channel has a unique name and an owner and may have zero or more
 * properties/tags associated with it.
 *
 * @author eschuhmacher
 *
 */
public class Shift {

    private Long id;
    private Date startDate;
    private Date endDate;
    private String owner;

    /**
     * Builder class to aid in a construction of a shift.
     *
     * @author eschuhmacher
     *
     */
    public static class Builder {
        private Long id;
        private Date startDate;
        private Date endDate;
        private String owner;

        /**
         * Create a shift builder initialized to a copy of the shift
         *
         * @param shift
         *            - the shift to be copied
         * @return channel {@link Builder} with all the attributes copied from
         *         the shift.
         */
        public static Builder shift(Shift shift) {
            Builder shiftBuilder = new Builder();
            shiftBuilder.id = shift.getId();
            shiftBuilder.owner = shift.getOwner();
            shiftBuilder.startDate = shift.getStartDate();
            shiftBuilder.endDate = shift.getEndDate();
            return shiftBuilder;
        }

        XMLShift toXml() {
            XMLShift xmlShift = new XMLShift(id, owner, startDate);
            xmlShift.setEndDate(endDate);
            return xmlShift;

        }

        /**
         * build a {@link Shift} object using this builder.
         *
         * @return a {@link Shift}
         */
        public Shift build() {
            return new Shift(this);
        }
    }

    Shift(XMLShift shift) {
        this.id = shift.getId();
        this.owner = shift.getOwner();
        this.startDate = shift.getStartDate();
        this.endDate = shift.getEndDate();
    }

    private Shift(Builder builder) {
        this.id = builder.id;
        this.owner = builder.owner;
        this.endDate = builder.endDate;
        this.startDate = builder.startDate;
    }


    /**
     * Returns the Id of the shift.
     *
     * @return shift Id.
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the owner of this Shift.
     *
     * @return owner name.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the start date of this Shift.
     *
     * @return start date.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Returns the end date of this Shift.
     *
     * @return end date.
     */
    public Date getEndDate() {
        return endDate;
    }





    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id.equals(null)) ? 0 : id.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Shift))
            return false;
        Shift other = (Shift) obj;
        if (id.equals(null)) {
            if (!other.id.equals(null))
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
