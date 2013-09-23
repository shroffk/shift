package gov.bnl.shift.api;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * @author: eschuhmacher
 */
@XmlRootElement(name = "shift")
public class XMLShift {
    private Long id;
    private String owner;
    private Date startDate;
    private Date endDate;

    /** Creates a new instance of XMLShift */
    public XMLShift() {
    }

    /**
     * Creates a new instance of XMLShift.
     *
     * @param id shift id
     */
    public XMLShift(Long id) {
        this.id = id;
    }

    /**
     * Creates a new instance of XmlChannel.
     *
     * @param id shift id
     * @param owner owner name
     * @param startDate
     */
    public XMLShift(Long id, String owner, Date startDate) {
        this.id = id;
        this.owner = owner;
        this.startDate = startDate;
    }

    /**
     * Getter for shift id.
     *
     * @return id
     */
    @XmlAttribute
    public Long getId() {
        return id;
    }

    /**
     * Setter for shift id.
     *
     * @param id the value to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for shift owner.
     *
     * @return owner
     */
    @XmlAttribute
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for shift owner.
     *
     * @param owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Getter for shift's startDate.
     *
     * @return Date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Setter for shift's startDate.
     *
     * @param startDate Date
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Getter for the shifts's endDate.
     *
     * @return Date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Setter for the shift's endDate.
     *
     * @param endDate
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data XmlShift to create the string representation for
     * @return string representation
     */
    public static String toLog(XMLShift data) {
        return data.getId() + "(" + data.getOwner() + "):["
                + data.getStartDate()
                + data.getStartDate()
                + "]";
    }
}