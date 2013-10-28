package gov.bnl.shiftClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author eschuhmacher
 *
 */
public class ShiftUtil {

    /**
     * This class is not meant to be instantiated or extended
     */
    private ShiftUtil(){

    }


    /**
     * Returns all the shift ids
     *
     * @param shifts
     * @return Integer collection of shifts ids
     */
    public static Collection<Integer> getShiftId(Collection<Shift> shifts) {
        Collection<Integer> shiftIds = new HashSet<Integer>();
        for (Shift shift : shifts) {
            shiftIds.add(shift.getId());
        }
        return shiftIds;
    }

    static Collection<Shift> toShifts(XmlShifts xmlShifts){
        Collection<Shift> shifts = new HashSet<Shift>();
        for (XmlShift xmlShift : xmlShifts.getShifts()) {
            shifts.add(new Shift(xmlShift));
        }
        return shifts;
    }

}