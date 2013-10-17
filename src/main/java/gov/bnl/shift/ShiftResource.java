package gov.bnl.shift;

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
     * based on a multi-parameter query specifiying patterns, id, from-to startDate or owner name
     * Only allow one date from the from and to query, if multiple dates appear on the url, the first ones will be the ones to use
     * "*" will be used as a wild card to retrive all the shifts
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
            Shifts result = shiftManager.findShiftsByMultiMatch(uriInfo.getQueryParameters());
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
     * GET method for retrieving an instance of Shift identified by the shift Id <tt>shiftId</tt>.
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
    public Response create(Shift newShift) {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        UserManager um = UserManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
        try {
            final Shift openShift = shiftManager.getOpenShift();
            if (openShift != null) {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                        "The shift " + openShift.getId() + " is still open, please continue using that shift or end it before trying to start a new one");
            }
            db.getConnection();
            db.beginTransaction();
            if (!um.userHasAdminRole()) {
                shiftManager.checkUserBelongsToGroup(um.getUserName(), newShift);
            }
            final Shift result = shiftManager.startShift(newShift);
            db.commit();
            Response r =  Response.ok(result).build();
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
    public Response endShift(Shift shift) {
        final DbConnection db = DbConnection.getInstance();
        final ShiftManager shiftManager = ShiftManager.getInstance();
        System.out.println(securityContext.getUserPrincipal());
        try {
            db.getConnection();
            db.beginTransaction();
            final Shift result = shiftManager.endShift(shift);
            db.commit();
            Response r =  Response.ok(result).build();
            audit.info(securityContext.getUserPrincipal().getName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
                    + "|data=" + Shift.toLogger(shift));
            return r;
        } catch (ShiftFinderException e) {
            return e.toResponse();
        } finally {
            db.releaseConnection();
        }
    }
}
