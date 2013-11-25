package gov.bnl.shift;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author eschuhmacher
 * Top level Jersey HTTP methods for the .../shifts URL
 *
 */

@Path("/shift/")
public class ShiftResource {
    @Context
    private UriInfo uriInfo;
    @Context
    private javax.ws.rs.core.SecurityContext securityContext;

    private Logger audit = Logger.getLogger(this.getClass().getPackage().getName() + ".audit");
    private Logger log = Logger.getLogger(this.getClass().getName());

    /** Creates a new instance of ShiftResource */
    public ShiftResource() {
    }

    /**
     * GET method for retrieving a collection of shift instances,
     *
     * @return HTTP Response
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public Response listAll() {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
        try {
            db.getConnection();
            db.beginTransaction();
            Shifts result;
            if(uriInfo.getQueryParameters() == null || uriInfo.getQueryParameters().isEmpty()) {
                result = shiftManager.listAllShifts();
            } else {
                result = shiftManager.findShiftsByMultiMatch(uriInfo.getQueryParameters());
            }
            db.commit();
            final Response r = Response.ok(result).build();
            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus()
                    + "|returns " + result.getShifts().size() + " shifts");
            return r;
        } catch (ShiftFinderException e) {
            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
                    + e.getResponseStatusCode() +  "|cause=" + e);
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * GET method for retrieving a collection of types instances,
     *
     * @return HTTP Rfesponse
     */
    @GET
    @Path("type")
    @Produces({"application/xml", "application/json"})
    public Response listTypes() throws ParserConfigurationException, IOException, SAXException {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
        try {
            db.getConnection();
            db.beginTransaction();
            final Types result = shiftManager.listTypes();
            db.commit();
            final Response r = Response.ok(result).build();
            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus()
                    + "|returns ");
            return r;
        } catch (ShiftFinderException e) {
            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
                    + e.getResponseStatusCode() + "|cause=" + e);
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * GET method for retrieving a collection of shift instances,
     * based on a multi-parameter query specifiying patterns, id, from-to startDate or owner name
     * Only allow one date from the from and to query, if multiple dates appear on the url, the first ones will be the ones to use
     * "*" will be used as a wild card to retrive all the shifts
     *
     * @return HTTP Response
     */
    @GET
    @Path("{type}")
    @Produces({"application/xml", "application/json"})
    public Response query(final @PathParam("type") String type) {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
        try {
            db.getConnection();
            db.beginTransaction();
            final MultivaluedMap<String, String> map = new MultivaluedMapImpl();
            map.add("type", type);
            final Shifts result = shiftManager.findShiftsByMultiMatch(map);
            db.commit();
            final Response r = Response.ok(result.getShiftList().iterator().next()).build();
            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus()
                    + "|returns " + result.getShifts().size() + " shifts");
            return r;
        } catch (ShiftFinderException e) {
            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
                    + e.getResponseStatusCode() +  "|cause=" + e);
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * GET method for retrieving an instance of Shift identified by the shift Id <tt>shiftId</tt>.
     *
     * @param shiftId shift id
     * @return HTTP Response
     */
    @GET
    @Path("{type}/{shiftId}")
    @Produces({"application/xml", "application/json"})
    public Response read(final @PathParam("type") String typeName, final @PathParam("shiftId") Integer shiftId) {
        audit.info("getting shift:" + shiftId);
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
        System.out.println(user);
        Shift result = null;
        try {
            db.getConnection();
            db.beginTransaction();
            result = shiftManager.findShiftById(shiftId);
            db.commit();
            Response r;
            if (result == null) {
                r = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                r = Response.ok(result).build();
            }
            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus());
            return r;
        } catch (ShiftFinderException e) {
            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
                    + e.getResponseStatusCode() +  "|cause=" + e);
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * PUT method for start  a new shift instance, throw an exception if any other shift is still open
     *
     * @param newShift
     * @return HTTP response
     */
    @PUT
    @Path("start")
    @Consumes({"application/xml", "application/json"})
    public Response create(final Shift newShift) {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final UserManager um = UserManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
        try {
            final Shift openShift = shiftManager.getOpenShift(newShift.getType().getName());
            if (openShift != null) {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                        "The shift " + openShift.getId() + " is still open, please continue using that shift or end it before trying to start a new one");
            }
            db.getConnection();
            db.beginTransaction();
//            if (!um.userHasAdminRole()) {
//                shiftManager.checkUserBelongsToGroup(um.getUserName(), newShift);
//            }
            final Shift result = shiftManager.startShift(newShift);
            db.commit();
            final Response r =  Response.ok(result).build();
            audit.info(securityContext.getUserPrincipal().getName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
                    + "|data=" + Shift.toLogger(newShift));
            return r;
        } catch (ShiftFinderException e) {
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * PUT method for end a shift instance.
     *
     * @param shift
     * @return HTTP response
     */
    @PUT
    @Path("end")
    @Consumes({"application/xml", "application/json"})
    public Response endShift(final Shift shift) {
        final UserManager um = UserManager.getInstance();
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
        try {
            db.getConnection();
            db.beginTransaction();
//            if (!um.userHasAdminRole()) {
//                shiftManager.checkUserBelongsToGroup(um.getUserName(), shift);
//            }
            final Shift result = shiftManager.endShift(shift);
            db.commit();
            final Response r =  Response.ok(result).build();
            audit.info(securityContext.getUserPrincipal().getName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
                    + "|data=" + Shift.toLogger(shift));
            return r;
        } catch (ShiftFinderException e) {
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * PUT method for close a shift instance.
     *
     * @param shift
     * @return HTTP response
     */
    @PUT
    @Path("close")
    @Consumes({"application/xml", "application/json"})
    public Response closeShift(final Shift shift) {
        final UserManager um = UserManager.getInstance();
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
        try {
            db.getConnection();
            db.beginTransaction();
//            if (!um.userHasAdminRole()) {
//                shiftManager.checkUserBelongsToGroup(um.getUserName(), shift);
//            }
            final Shift result = shiftManager.closeShift(shift, um.getUserName());
            db.commit();
            final Response r =  Response.ok(result).build();
            audit.info(securityContext.getUserPrincipal().getName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
                    + "|data=" + Shift.toLogger(shift));
            return r;
        } catch (ShiftFinderException e) {
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    public void setSecurityContext(final javax.ws.rs.core.SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    public void setUriInfo(final UriInfo info) {
        this.uriInfo = info;
    }
}
