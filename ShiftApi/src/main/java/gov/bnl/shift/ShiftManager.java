package gov.bnl.shift;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    public Shift findShiftById(final Integer shiftId, final String type) throws ShiftFinderException {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        final CriteriaQuery<Shift> select = cq.select(from);
        final Predicate idPredicate = cb.equal(from.get("id"), shiftId);
        final Predicate typePredicate = cb.equal(from.get("type"), type);
        select.where(cb.and(idPredicate, typePredicate));
        cq.orderBy(cb.desc(from.get(Shift_.startDate)));
        final TypedQuery<Shift> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            Shift result = null;
            final List<Shift> rs = typedQuery.getResultList();
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
    public Shifts findShiftsByMultiMatch(final String type, final MultivaluedMap<String, String> matches) throws ShiftFinderException {
        final List<String> shift_ids = new LinkedList<String>();
        final List<String> shift_owners = new LinkedList<String>();
        final List<String> descriptions = new LinkedList<String>();
        final List<String> leadOperators = new LinkedList<String>();
        final List<String> onShiftOperators = new LinkedList<String>();
        final List<String> types = new LinkedList<String>();
        final List<String> closeUsers = new LinkedList<String>();
        String shift_start_date = null;
        String shift_end_date = null;
        types.add(type);
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
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
            } else if (key.equals("description")) {
                descriptions.addAll(match.getValue());
            } else if (key.equals("type")) {
                types.addAll(match.getValue());
            } else if (key.equals("leadOperator")) {
                leadOperators.addAll(match.getValue());
            } else if (key.equals("onShiftPersonal")) {
                onShiftOperators.addAll(match.getValue());
            } else if (key.equals("closeUser")) {
                onShiftOperators.addAll(match.getValue());
            }
        }
        Predicate idPredicate = cb.disjunction();
        if (!shift_ids.isEmpty()) {
        idPredicate = cb.or(from.get(Shift_.id).in(shift_ids), idPredicate);
        }
        Predicate ownerPredicate = cb.disjunction();
        if(!shift_owners.isEmpty()) {
            ownerPredicate = cb.or(from.get(Shift_.owner).in(shift_owners), ownerPredicate);
        }
        Predicate descriptionPredicate = cb.disjunction();
        if(!descriptions.isEmpty()) {
            ownerPredicate = cb.or(from.get(Shift_.description).in(descriptions), descriptionPredicate);
        }
        Predicate typenPredicate = cb.disjunction();
        if(!types.isEmpty()) {
            typenPredicate = cb.or(from.get(Shift_.type).in(types), typenPredicate);
        }
        Predicate leadPredicate = cb.disjunction();
        if(!leadOperators.isEmpty()) {
            leadPredicate = cb.or(from.get(Shift_.leadOperator).in(leadOperators), leadPredicate);
        }
        Predicate onShiftPersonalPredicate = cb.disjunction();
        if(!onShiftOperators.isEmpty()) {
            onShiftPersonalPredicate = cb.or(from.get(Shift_.onShiftPersonal).in(onShiftOperators), onShiftPersonalPredicate);
        }
        Predicate closeUserPredicate = cb.disjunction();
        if(!closeUsers.isEmpty()) {
            closeUserPredicate = cb.or(from.get(Shift_.closeShiftUser).in(closeUserPredicate), closeUserPredicate);
        }
        Predicate datePredicate = cb.disjunction();
        if(shift_end_date != null || shift_start_date != null) {
        if (shift_start_date != null && shift_end_date == null) {
            final Date jStart = new java.util.Date(Long.valueOf(shift_start_date) * 1000);
            final Date jEndNow = new java.util.Date(Calendar.getInstance().getTime().getTime());
            datePredicate = cb.between(from.get(Shift_.startDate),
                        jStart,
                        jEndNow);
        } else if (shift_start_date == null && shift_end_date != null) {
            final Date jStart1970 = new java.util.Date(0);
            final Date jEnd = new java.util.Date(Long.valueOf(shift_end_date) * 1000);
            datePredicate = cb.between(from.get(Shift_.startDate),
                    jStart1970,
                    jEnd);
        } else {
            final Date jStart = new java.util.Date(Long.valueOf(shift_start_date) * 1000);
            final Date jEnd = new java.util.Date(Long.valueOf(shift_end_date) * 1000);
            datePredicate = cb.between(from.get(Shift_.startDate),
                    jStart,
                    jEnd);
        }
        }
        final Predicate finalPredicate = cb.and(idPredicate, ownerPredicate, descriptionPredicate, typenPredicate,
                datePredicate, leadPredicate, onShiftPersonalPredicate, closeUserPredicate);
        cq.where(finalPredicate);
        cq.orderBy(cb.desc(from.get(Shift_.startDate)));
        final TypedQuery<Shift> typedQuery = em.createQuery(cq);
        JPAUtil.startTransaction(em);
        if(matches.isEmpty()) {
            typedQuery.setMaxResults(1);
        }
        try {
            final Shifts result = new Shifts();
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
        final UserManager um = UserManager.getInstance();
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
            final Shift existingShift = findShiftById(shift.getId(), shift.getType());
            existingShift.setEndDate(new Date());
            JPAUtil.update(existingShift);
            return existingShift;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        }
    }


    /**
     * Add the user that close the shift
     * specified in the Shift <tt>shift</tt>.
     *
     *
     * @param shift Shift shift
     * @throws ShiftFinderException on ownership mismatch, or wrapping an SQLException
     */
    public Shift closeShift(final Shift shift, final String user) throws ShiftFinderException {
        try {
            final Shift existingShift = findShiftById(shift.getId(), shift.getType());
            existingShift.setCloseShiftUser(user);
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
    public Shift getOpenShift(final String shiftType) throws ShiftFinderException {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        final CriteriaQuery<Shift> select = cq.select(from);
        final Predicate typePredicate = cb.equal(from.get("type"), shiftType);
        final Predicate endDatePredicate = cb.isNull(from.get("endDate"));
        final Predicate finalPredicate = cb.and(typePredicate, endDatePredicate);
        select.where(finalPredicate);
        cq.orderBy(cb.asc(from.get(Shift_.startDate)));
        final TypedQuery<Shift> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            Shift result = null;
            final List<Shift> rs = typedQuery.getResultList();
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

    public Shifts listAllShifts() {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        final CriteriaQuery<Shift> select = cq.select(from);
        cq.orderBy(cb.asc(from.get(Shift_.startDate)));
        final TypedQuery<Shift> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            Shifts result = new Shifts();
            final List<Shift> rs = typedQuery.getResultList();
            if (rs != null) {
                Iterator<Shift> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    result.addShift(iterator.next());
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

    public String listTypes() {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        JPAUtil.startTransaction(em);
        try {
            StringBuilder result = new StringBuilder();
            final List<String> rs = em.createNamedQuery("Select distinct(type) from shift").getResultList();
            if (rs != null) {
                Iterator<String> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    result.append(iterator.next()).append(",");
                }
            }

            return result.substring(0, result.length() - 1);
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        } finally {
            JPAUtil.finishTransacton(em);
        }        }
}
