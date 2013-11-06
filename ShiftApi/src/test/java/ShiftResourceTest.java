import gov.bnl.shift.Shift;
import gov.bnl.shift.Shifts;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShiftResourceTest {

    private static final URI BASE_URI = getBaseURI();
    private WebResource httpResource;
    private WebResource httpsResource;

    private static int getPort(int defaultPort) {
        String port = System.getenv("JERSEY_HTTP_PORT");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;
    }

    private static int getHttpsPort(int defaultPort) {
        String port = System.getenv("JERSEY_HTTPS_PORT");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost/").port(getPort(8080))
                .path("Shift").build();
    }

    private static URI getBaseHttpsURI() {
        return UriBuilder.fromUri("https://localhost/").port(getHttpsPort(8181))
                .path("Shift").build();
    }



    @Before
    public void setUp() throws Exception {
        Client c = Client.create();
        httpResource = c.resource(BASE_URI);
        c.addFilter(new HTTPBasicAuthFilter("shift", "shift"));
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(new KeyManager[0], new TrustManager[] {new DummyX509TrustManager()}, new SecureRandom());
        SSLContext.setDefault(ssl);
        HTTPSProperties prop = new HTTPSProperties(null, ssl);
        DefaultClientConfig dcc = new DefaultClientConfig();
        dcc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);
        Client c2 = Client.create(dcc);
        c2.addFilter(new HTTPBasicAuthFilter("shift", "shift"));
        httpsResource = c2.resource(getBaseHttpsURI());

    }

    @Test
    public void testAddShift() throws IOException {
        Shift shift = new Shift();
        shift.setOwner("shift");
        shift.setType("testType");
        shift.setDescription("test case shift");
        Date startDate = new Date();
        Shift addedShift = httpsResource.path("/resources/shift/start").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, shift);
        assertEquals(addedShift.getOwner(), shift.getOwner());
        assertEquals(addedShift.getType(), shift.getType());
        assertTrue(addedShift.getStartDate().compareTo(startDate) >= 0);
        Shifts queryShifts =  httpResource.path("/resources/shift/testType").accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertEquals(queryShifts.getFirst().getId(), addedShift.getId());
    }

    @Test
    public void testEndShift() {
        Shifts queryShifts =  httpResource.path("/resources/shift/testType").accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertTrue(queryShifts.getFirst().getEndDate() == null);
        Shift endShift = httpsResource.path("/resources/shift/end").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, queryShifts.getFirst());
        assertTrue(endShift.getEndDate() != null);
        assertEquals(endShift.getStartDate(), queryShifts.getFirst().getStartDate());
    }

    @Test
    public void testAddMultipleShifts() {
        Shift shift = new Shift();
        shift.setOwner("shift");
        shift.setType("testType");
        shift.setDescription("test case shift");
        Date startDate = new Date();
        Shift shift2 = new Shift();
        shift2.setOwner("shift");
        shift2.setType("testType2");
        shift2.setDescription("test case shift2");
        Shift addedShift = httpsResource.path("/resources/shift/start").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, shift);
        Shift addedShift2 = httpsResource.path("/resources/shift/start").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, shift2);
        assertTrue(addedShift.getId() != addedShift2.getId());
        assertEquals(shift.getType(), addedShift.getType());
        assertEquals(shift2.getType(), addedShift2.getType());
        Shift endShift = httpsResource.path("/resources/shift/end").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, addedShift);
        Shift endShift2 = httpsResource.path("/resources/shift/end").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, addedShift2);
        assertTrue(endShift.getEndDate() != null);
        assertTrue(endShift2.getEndDate() != null);

    }

    @Test(expected = UniformInterfaceException.class)
    public void testAddSameTypeOfShiftTwice() {
        Shift addedShift = null;
        try {
            Shift shift = new Shift();
            shift.setOwner("shift");
            shift.setType("testType");
            shift.setDescription("test case shift");
            Date startDate = new Date();
            Shift shift2 = new Shift();
            shift2.setOwner("shift");
            shift2.setType("testType");
            shift2.setDescription("test case shift2");
            addedShift = httpsResource.path("/resources/shift/start").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, shift);
            Shift addedShift2 = httpsResource.path("/resources/shift/start").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, shift2);
        } finally {
            httpsResource.path("/resources/shift/end").accept(MediaType.APPLICATION_XML).type("application/xml").put(Shift.class, addedShift);
        }
    }

    @Test
    public void testSearch() {
        Shifts shift = httpResource.path("/resources/shift/testtype").accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertEquals(shift.getFirst().getOwner(), "shift");
        assertEquals(1, shift.size());
        Shift sameShift = httpResource.path(("/resources/shift/testtype/" + shift.getFirst().getId()) ).accept(MediaType.APPLICATION_XML).get(Shift.class);
        assertEquals(sameShift, shift.getFirst());
    }

    @Test
    public void findShiftByMultipleParamethers() {
        FormDataMultiPart form = new FormDataMultiPart();
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.add("owner", "shift");
        Shifts shifts23 = httpResource.path("/resources/shift").accept(MediaType.APPLICATION_XML).get(Shifts.class);
        Shifts shifts = httpResource.path("/resources/shift/testtype").queryParams(map).accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertTrue(shifts.size() >= 4);
        map.clear();
        map.add("owner", "XXX");
        Shifts shifts2 = httpResource.path("/resources/shift/testtype").queryParams(map).accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertEquals(0, shifts2.size());
        map.clear();
        map.add("to",  String.valueOf(new Date().getTime()));
        Shifts shift3  = httpResource.path("/resources/shift/testtype").queryParams(map).accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertTrue(shift3.size() >= 4);
        map.clear();
        map.add("from",  String.valueOf(new Date().getTime()));
        Shifts shift4  = httpResource.path("/resources/shift/testtype").queryParams(map).accept(MediaType.APPLICATION_XML).get(Shifts.class);
        assertEquals(shift4.size(), 0);
    }



}

