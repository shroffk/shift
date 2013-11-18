package gov.bnl.shift;

import com.sun.jersey.api.client.WebResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.security.Principal;


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.easymock.PowerMock.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DbConnection.class,ShiftManager.class})

public class ShiftServiceTest  {




    public static final String PACKAGE_NAME = "gov.bnl.shift";
    private WebResource ws;



    @Test
    public void testShiftResponse() throws UnsupportedEncodingException {
        Shift shift = new Shift(12);
        Shifts shifts = new Shifts(shift);
        ShiftResource shiftResource = preprareTest(shifts);
        Response listOfShifts = shiftResource.listAll();
        assertEquals(200, listOfShifts.getStatus());
        assertEquals((Object) 12, ((Shifts) listOfShifts.getEntity()).get(0).getId());

    }

    @Test
    public void testFindShiftById() throws UnsupportedEncodingException {
        Shift shift = new Shift(12);
        Shifts shifts = new Shifts(shift);
        ShiftResource shiftResource = preprareTest(shifts);
        Response response = shiftResource.read("test", 12);
        assertEquals(200, response.getStatus());
        assertEquals((Object) 12, ((Shift) response.getEntity()).getId());

    }

    @Test
    public void testStartShift() throws UnsupportedEncodingException {
        Shift shift = new Shift(12);
        Type type = new Type();
        type.setName("test");
        shift.setType(type);
        Shifts shifts = new Shifts(shift);
        ShiftResource shiftResource = preprareTest(shifts);
        Response response = shiftResource.create(shift);
        assertEquals(200, response.getStatus());
        assertEquals((Object) 12, ((Shift) response.getEntity()).getId());

    }

    @Test
    public void testEndShift() throws UnsupportedEncodingException {
        Shift shift = new Shift(12);
        Type type = new Type();
        type.setName("test");
        shift.setType(type);
        Shifts shifts = new Shifts(shift);
        ShiftResource shiftResource = preprareTest(shifts);
        Response response = shiftResource.endShift(shift);
        assertEquals(200, response.getStatus());
        assertEquals((Object) 12, ((Shift) response.getEntity()).getId());

    }

    private ShiftResource preprareTest(final Shifts shifts) {
        DbConnection mockConnection = mock(DbConnection.class);
        ShiftManager manager = mock(ShiftManager.class);
        javax.ws.rs.core.SecurityContext securityContext = mock(javax.ws.rs.core.SecurityContext.class);
        UriInfo info = mock(UriInfo.class);
        mockStatic(ShiftManager.class);
        mockStatic(DbConnection.class);
        expect(DbConnection.getInstance()).andReturn(mockConnection);
        expect(ShiftManager.getInstance()).andReturn(manager);
        when(securityContext.isUserInRole("Administrator")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "shift";
            }
        });
        when(info.getQueryParameters()).thenReturn(null);
        when(info.getPath()).thenReturn(null);
        mockConnection.beginTransaction();
        expectLastCall().times(1);
        when(mockConnection.getConnection()).thenReturn(null);
        when(manager.listAllShifts()).thenReturn(shifts);
        when(manager.findShiftById(12)).thenReturn(shifts.get(0));
        when(manager.startShift(shifts.get(0))).thenReturn(shifts.get(0));
        when(manager.endShift(shifts.get(0))).thenReturn(shifts.get(0));
        PowerMock.replay(DbConnection.class);
        PowerMock.replay(ShiftManager.class);
        ShiftResource shiftResource = new ShiftResource();
        shiftResource.setSecurityContext(securityContext);
        shiftResource.setUriInfo(info);
        return shiftResource;
    }


}
