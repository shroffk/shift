package gov.bnl.shift;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author eschuhmacher
 */
public class ShiftManager {

    private static EntityManager em = null;

    private static ShiftManager instance = new ShiftManager();

    /**
     * Create an instance of ShiftManager
     */
    private ShiftManager() {
    }

    /**
     * Returns the (singleton) instance of ShiftManager
     *
     * @return the instance of ShiftManager
     */
    public static ShiftManager getInstance() {
        return instance;
    }

    /**
     * Return single shift found by shift id.
     *
     *
     * @param shiftId id to look for
     * @return Shift with found shift
     * @throws ShiftFinderException on SQLException
     */
    public Shift findShiftById(final String shiftId) throws ShiftFinderException {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        Root<Shift> from = cq.from(Shift.class);
        CriteriaQuery<Shift> select = cq.select(from);
        Predicate idPredicate = cb.equal(from.get("id"), shiftId);
        select.where(idPredicate);
        select.orderBy(cb.asc(from.get("startDate")));
        TypedQuery<Shift> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            Shift result = null;
            List<Shift> rs = typedQuery.getResultList();
            if (rs != null) {
                Iterator<Shift> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    result = iterator.next();
                }
            }

            return result;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        } finally {
            JPAUtil.finishTransacton(em);
        }
    }


    /**
     * Returns multiple shifts found by matching ids, start dates or owner name.
     *
     * @param matches multivalued map of patterns to match
     * their values against.
     * @return Shifts container
     * @throws ShiftFinderException wrapping an SQLException
     */
    public Shifts findShiftsByMultiMatch(final MultivaluedMap<String, String> matches) throws ShiftFinderException {
        List<String> shift_ids = new LinkedList<String>();
        List<String> shift_owners = new LinkedList<String>();
        List<String> descriptions = new LinkedList<String>();
        List<String> leadOperators = new LinkedList<String>();
        List<String> reports = new LinkedList<String>();
        List<String> onShiftOperators = new LinkedList<String>();
        String shift_start_date;
        String shift_end_date;
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        Root<Shift> from = cq.from(Shift.class);
        for (final Map.Entry<String, List<String>> match : matches.entrySet()) {
            final String key = match.getKey().toLowerCase();
            if (key.equals("id")) {
                shift_ids.addAll(match.getValue());
                //Only allow one date to query for from and to, if multiple ones appear on the query only use the first one
            } else if(key.equals("from")) {
                shift_start_date = match.getValue().get(0);
                //Only allow one date to query for from and to, if multiple ones appear on the query only use the first one
            } else if (key.equals("to")) {
                shift_end_date = match.getValue().get(0);
            } else if (key.equals("owner")) {
                shift_owners.addAll(match.getValue());
            }
        }
        Predicate shiftPredicate = cb.disjunction();
        if (!shift_ids.isEmpty()) {
        shiftPredicate = cb.or(from.get(Shift_.id).in(shift_ids), shiftPredicate);
        }
        if(!shift_owners.isEmpty()) {
            shiftPredicate = cb.or(from.get(Shift_.owner).in(shift_owners), shiftPredicate);
        }
        cq.where(shiftPredicate);
        cq.orderBy(cb.desc(from.get(Shift_.startDate)));
        TypedQuery<Shift> typedQuery = em.createQuery(cq);
        JPAUtil.startTransaction(em);

        try {
            Shifts result = new Shifts();
            List<Shift> rs = typedQuery.getResultList();

            if (rs != null) {
                Iterator<Shift> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    Shift shift = iterator.next();
                    result.addShift(shift);
                }
            }

            return result;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        } finally {
            JPAUtil.finishTransacton(em);
        }

    }

    /**
     * Check that <tt>user</tt> belongs to the owner group specified in the
     * shift <tt>shift</tt>.
     *
     * @param user user name
     * @param shift Shift shift to check ownership for
     * @throws ShiftFinderException on name mismatch
     */
    public void checkUserBelongsToGroup(String user, Shift shift) throws ShiftFinderException {
        if (shift == null) return;
        UserManager um = UserManager.getInstance();
        if (!um.userIsInGroup(shift.getOwner())) {
            throw new ShiftFinderException(Response.Status.FORBIDDEN,
                    "User '" + um.getUserName()
                            + "' does not belong to owner group '" + shift.getOwner()
                            + "' of shift '" + shift.getId() + "'");
        }
    }

    /**
     * Add the end Date to a shift
     * specified in the Shift <tt>shift</tt>.
     *
     *
     * @param shift Shift shift to end
     * @throws ShiftFinderException on ownership mismatch, or wrapping an SQLException
     */
    public Shift endShift(final Shift shift) throws ShiftFinderException {
        try {
            Shift existingShift = findShiftById(String.valueOf(shift.getId()));
            existingShift.setEndDate(new Date());
            JPAUtil.update(existingShift);
            return existingShift;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        }
    }

    /**
     * Find the last open shift
     * @throws ShiftFinderException
     */
    public Shift getOpenShift() throws ShiftFinderException {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        Root<Shift> from = cq.from(Shift.class);
        CriteriaQuery<Shift> select = cq.select(from);
        Predicate datePredicate = cb.equal(from.get("end_date"), null);
        select.where(datePredicate);
        select.orderBy(cb.asc(from.get("startDate")));
        TypedQuery<Shift> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            Shift result = null;
            List<Shift> rs = typedQuery.getResultList();
            if (rs != null) {
                Iterator<Shift> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    result = iterator.next();
                }
            }

            return result;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        } finally {
            JPAUtil.finishTransacton(em);
        }        }

    /**
     * Create a new Shift instance
     * @param shift Shift
     * @throws ShiftFinderException
     */
    public Shift startShift(final Shift shift) throws ShiftFinderException {
        try {
            shift.setStartDate(new Date());
            JPAUtil.save(shift);
            return shift;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        }
    }

}
