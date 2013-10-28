package gov.bnl.shiftClient;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import gov.bnl.shiftClient.ShiftClientImpl.ShiftClientBuilder;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
*@author: eschuhmacher
 */
public class ShiftClientTest {

    private static ShiftClient client;
    private final String type = "testType";

    @BeforeClass
    public static void setup() {
        try {
            client = ShiftClientBuilder.serviceURL()
                    .withHTTPAuthentication(true).username("shift").password("shift").create();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void startShiftTest() {
        Shift shift = new Shift();
        shift.setOwner("shiftTestCase");
        shift.setType(type);
        Shift addedShift = client.start(shift);
        assertEquals(addedShift.getOwner(), shift.getOwner());
        assertTrue(addedShift.getId() != null);
    }

    @Test
    public void endShiftTest() {
        Shift shift = client.listShifts(type).iterator().next();
        Shift endedShift = client.end(shift);
        assertEquals(endedShift, shift);
    }

    @Test
    public void getShiftsTest() {
        Collection<Shift> shifts = client.listShifts("testType");
        assertTrue(shifts.size() > 0);
    }

    @Test
    public void getShiftByMap() {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("owner", "shift");
        Collection<Shift> shifts1 = client.findShifts(map, type);
        map.add("to",  String.valueOf(new Date().getTime()));
        Collection<Shift> shifts2 =  client.findShifts(map, type);
        assertEquals(shifts1.size(), shifts2.size());
    }
}
