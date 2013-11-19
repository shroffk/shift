package gov.bnl.shift;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.LinkedList;
import java.util.List;

/**
*@author :eschuhmacher
 */
@XmlRootElement(name = "shifts")
public class Shifts extends LinkedList<Shift> {

    public Shifts() {
    }

    public Shifts(Shift shift) {
        this.add(shift);
    }

    public Shifts(List<Shift> shifts) {
        this.addAll(shifts);
    }

    @XmlElementRef(type = Shift.class, name = "shift")
    public List<Shift> getShiftList() {
        return this;
    }

    @XmlTransient
    public List<Shift> getShifts() {
        return this;

    }

    public void setShifts(List<Shift> shifts) {
        this.addAll(shifts);
    }

    public void addShift(Shift shift) {
        this.add(shift);
    }


    /**
     * Creates a compact string representation for the shift.
     *
     * @param data Shift to create the string representation for
     * @return string representation
     */
    public static String toLogger(Shifts data) {
        if (data.getShifts().size() == 0) {
            return "[None]";
        } else {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (Shift c : data.getShifts()) {
                s.append(Shift.toLogger(c) + ",");
            }
            s.delete(s.length() - 1, s.length());
            s.append("]");
            return s.toString();
        }
    }

    public static String toLogger(List<Shift> data) {
        if (data.size() == 0) {
            return "[None]";
        } else {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (Shift c : data) {
                s.append(Shift.toLogger(c) + ",");
            }
            s.delete(s.length() - 1, s.length());
            s.append("]");
            return s.toString();
        }
    }
}
