package gov.bnl.shift;

import javax.persistence.metamodel.SingularAttribute;
import java.util.Date;

public class Shift_ {
    public static volatile SingularAttribute<Shift, Long> id;
    public static volatile SingularAttribute<Shift, String> owner;
    public static volatile SingularAttribute<Shift, Date> startDate;
}
