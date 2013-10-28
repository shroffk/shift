package gov.bnl.shiftClient;

import java.util.Date;

/**
*@author: eschuhmacher
 */
public class ShiftBuilder {
    private Integer id;
    private String type;
    private String owner;
    private Date startDate;
    private Date endDate;
    private String description;
    private String leadOperator;
    private String onShiftPersonal;
    private String report;
    private String closeShiftUser;

    public ShiftBuilder() {};

    public static ShiftBuilder shift(Shift shift) {
        ShiftBuilder shiftBuilder = new ShiftBuilder();
        shiftBuilder.id = shift.getId();
        shiftBuilder.type = shift.getType();
        shiftBuilder.owner = shift.getOwner();
        shiftBuilder.startDate = shift.getStartDate();
        shiftBuilder.endDate = shift.getEndDate();
        shiftBuilder.description = shift.getDescription();
        shiftBuilder.leadOperator = shift.getLeadOperator();
        shiftBuilder.onShiftPersonal = shift.getOnShiftPersonal();
        shiftBuilder.report = shift.getReport();
        shiftBuilder.closeShiftUser = shift.getCloseShiftUser();
        return shiftBuilder;
    }

    public static ShiftBuilder shift() {
        ShiftBuilder shiftBuilder = new ShiftBuilder();
        return shiftBuilder;
    }

    public ShiftBuilder(Integer id) {
        this.id = id;
    }

}
