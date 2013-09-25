package bnl.gov.shift;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

/**
 *
 * @author eschuhmacher
 */

@Path("/shift/")
public class ShiftResource {
    @Context
    private UriInfo uriInfo;
    @Context
    private javax.ws.rs.core.SecurityContext securityContext;

    private Logger audit = Logger.getLogger(this.getClass().getPackage().getName() + ".audit");
    private Logger log = Logger.getLogger(this.getClass().getName());
    private final String chNameRegex = "[^\\s]+";

    /** Creates a new instance of ShiftResource */
    public ShiftResource() {
    }

    /**
     * GET method for retrieving a collection of shift instances,
     * based on a multi-parameter query specifiying patterns
     *
     * @return HTTP Response
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public Response query() {
        DbConnection db = DbConnection.getInstance();
        ShiftManager shiftManager = ShiftManager.getInstance();
        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
        try {
            db.getConnection();
            db.beginTransaction();
            XMLShifts result = shiftManager.findShiftsByMultiMatch(uriInfo.getQueryParameters());
            db.commit();
            Response r = Response.ok(result).build();
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
     * GET method for retrieving an instance of Shift identified by <tt>shiftId</tt>.
     *
     * @param shiftId shift id
     * @return HTTP Response
     */
    @GET
    @Path("{shiftId}")
    @Produces({"application/xml", "application/json"})
    public Response read(@PathParam("shiftId") String shiftId) {
        audit.info("getting shift:" + shiftId);
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
        XMLShift result = null;
        try {
            db.getConnection();
            db.beginTransaction();
            result = shiftManager.findShiftById(Long.parseLong(shiftId));
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
     * PUT method for creating a shift instance
     *
     * @param newShift
     * @return HTTP response
     */
    @PUT
    @Path("start")
    @Consumes({"application/xml", "application/json"})
    public Response create(XMLShift newShift) {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final UserManager um = UserManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
        try {
            if (shiftManager.getOpenShift() != null) {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                        "The shift " + shiftManager.getOpenShift().getId() + " is still open, please end it before trying to start a new one");
            }
            db.getConnection();
            db.beginTransaction();
            shiftManager.startShift(newShift);
            db.commit();
            Response r = Response.noContent().build();
            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
                    + "|data=" + XMLShift.toLog(newShift));
            return r;
        } catch (ShiftFinderException e) {
            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|ERROR|" + e.getResponseStatusCode()
                    + "|data=" + XMLShift.toLog(newShift) + "|cause=" + e);
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }

    /**
     * PUT method for creating a shift instance
     *
     * @param shift
     * @return HTTP response
     */
    @PUT
    @Path("end")
    @Consumes({"application/xml", "application/json"})
    public Response endShift(XMLShift shift) {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        final UserManager um = UserManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
        try {
            db.getConnection();
            db.beginTransaction();
            shiftManager.endShift(shift);
            db.commit();
            Response r = Response.noContent().build();
            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
                    + "|data=" + XMLShift.toLog(shift));
            return r;
        } catch (ShiftFinderException e) {
            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|ERROR|" + e.getResponseStatusCode()
                    + "|data=" + XMLShift.toLog(shift) + "|cause=" + e);
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }
}
