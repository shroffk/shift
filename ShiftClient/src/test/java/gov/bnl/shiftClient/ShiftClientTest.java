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
    private final String type = "test";

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
        shift.setOwner("eschuhmacher");
        Type type = new Type();
        type.setId(1);
        type.setName("test");
        shift.setType(type);
        Shift addedShift = client.start(shift);
        assertEquals(addedShift.getOwner(), shift.getOwner());
        assertTrue(addedShift.getId() != null);

    }

    @Test
    public void endShiftTest() {
        Shift shift = client.getLastOpenShift("test");
        assertEquals("Active",shift.getStatus());
        Shift endedShift = client.end(shift);
        assertEquals(endedShift, shift);
    }

    @Test
    public void getShiftsTest() {
        Collection<Shift> shifts = client.listShifts();
        assertTrue(shifts.size() > 0);
    }

    @Test
    public void getShiftByMap() {
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("owner", "shift");
        Collection<Shift> shifts1 = client.findShifts(map);
        map.add("to",  String.valueOf(new Date().getTime() /1000));
        Collection<Shift> shifts2 =  client.findShifts(map);
        assertEquals(shifts1.size(), shifts2.size());
    }

    @Test
    public void getListTypes() {
        Collection<Type> types = client.listTypes();
        assertTrue(types.size() > 0);
        assertTrue(types.iterator().next().getName() != null);
    }

    @Test
    public void getLastOpenShift() {
        Shift shift = client.getLastOpenShift(type);
        assertEquals(type, shift.getType().getName());
    }
}
