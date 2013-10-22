package gov.bnl.shift;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import javax.xml.bind.annotation.*;

/**
*@author: eschuhmacher
 */

@Entity
@Table(name = "shift")
@XmlType(propOrder = {"id", "type", "owner", "startDate", "endDate", "description", "leadOperator", "onShiftPersonal", "report", "closeShiftUser"})
@XmlRootElement(name = "shift")
public class Shift implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id = null;

    @Column(name = "type", nullable = false, length = 250, insertable = true)
    private String type = null;

    @Column(name = "owner", nullable = false, length = 250, insertable = true)
    private String owner = null;

    @Column(name = "start_date", nullable = false, length = 250, insertable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate = null;

    @Column(name = "end_date", nullable = true, length = 250, insertable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate = null;

    @Column(name = "description", nullable = true, length = 250, insertable = true)
    private String description = null;

    @Column(name = "lead_operator", nullable = true, length = 250, insertable = true)
    private String leadOperator = null;

    @Column(name = "on_shift_personal", nullable = true, length = 250, insertable = true)
    private String onShiftPersonal = null;

    @Column(name = "report", nullable = true, length = 250, insertable = true)
    private String report = null;
    //TODO: check if electronic signature is better for this
    @Column(name = "close_shift_user", nullable = true, length = 250, insertable = true)
    private String closeShiftUser = null;

    public Shift() {
    }

    public Shift(Integer id) {
        this.id = id;
    }

    /**
     * Getter for shift id.
     *
     * @return id shift id
     */
    @XmlElement
    public Integer getId() {
        return id;
    }

    /**
     * Setter for shift id.
     *
     * @param id shift id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Getter for shift owner.
     *
     * @return owner shift owner
     */
    @XmlAttribute
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for shift owner.
     *
     * @param owner shift owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }
    /**
     * Getter for shift description.
     *
     * @return description shift description
     */
    @XmlAttribute
    public String getDescription() {
        return description;
    }

    /**
     * Setter for shift description.
     *
     * @param description shift description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for shift lead Operator.
     *
     * @return leadOperator shift lead operator
     */
    @XmlAttribute
    public String getLeadOperator() {
        return leadOperator;
    }

    /**
     * Setter for shift lead operator.
     *
     * @param leadOperator shift lead operator
     */
    public void setLeadOperator(String leadOperator) {
        this.leadOperator = leadOperator;
    }

    /**
     * Getter for shift onShiftPersonal.
     *
     * @return onShiftPersonal shift on shift personal
     */
    @XmlAttribute
    public String getOnShiftPersonal() {
        return onShiftPersonal;
    }

    /**
     * Setter for shift onShiftPersonal.
     *
     * @param onShiftPersonal shift on shift personal
     */
    public void setOnShiftPersonal(String onShiftPersonal) {
        this.onShiftPersonal = onShiftPersonal;
    }

    /**
     * Getter for shift report.
     *
     * @return report shift report
     */
    @XmlAttribute
    public String getReport() {
        return report;
    }

    /**
     * Setter for shift report.
     *
     * @param report shift report
     */
    public void setReport(String report) {
        this.report = report;
    }

    /**
     * Getter for shift type.
     *
     * @return type shift type
     */
    @XmlAttribute
    public String getType() {
        return type;
    }

    /**
     * Setter for shift tyoe.
     *
     * @param type shift type
     */
    public void setType(String type) {
        this.type = type;
    }
    /**
     * Getter for shift startDate.
     *
     * @return startDate shift start date
     */
    @XmlAttribute
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Setter for shift startDate.
     *
     * @param startDate shift start date
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Getter for shift endDate.
     *
     * @return endDate shift end date
     */
    @XmlAttribute
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Setter for shift endDate.
     *
     * @param endDate shift end date
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }


    /**
     * Getter for shift end shift user.
     *
     * @return closeShiftUser shift closeShiftUser
     */
    @XmlAttribute
    public String getCloseShiftUser() {
        return closeShiftUser;
    }

    /**
     * Setter for shift closeShiftUser.
     *
     * @param closeShiftUser shift type
     */
    public void setCloseShiftUser(String closeShiftUser) {
        this.closeShiftUser = closeShiftUser;
    }


    public int compareTo(Shift num) {
        int x = startDate.compareTo(num.startDate);
        return x;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data Log to create the string representation for
     * @return string representation
     */
    public static String toLogger(Shift data) {
        return data.getId() + ", " + data.getOwner();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0
                : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;

        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Shift other = (Shift) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }


}
