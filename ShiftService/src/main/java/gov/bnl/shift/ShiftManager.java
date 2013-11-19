package gov.bnl.shift;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
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
    public Shift findShiftById(final Integer shiftId) throws ShiftFinderException {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        final CriteriaQuery<Shift> select = cq.select(from);
        final Predicate idPredicate = cb.equal(from.get("id"), shiftId);
        select.where(cb.and(idPredicate));
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
    public Shifts findShiftsByMultiMatch(final MultivaluedMap<String, String> matches) throws ShiftFinderException {
        final List<String> shift_ids = new LinkedList<String>();
        final List<String> shift_owners = new LinkedList<String>();
        final List<String> descriptions = new LinkedList<String>();
        final List<String> leadOperators = new LinkedList<String>();
        final List<String> onShiftOperators = new LinkedList<String>();
        final List<Integer> typeIds = new LinkedList<Integer>();
        final List<String> closeUsers = new LinkedList<String>();
        String status = null;
        String shift_start_date = null;
        String shift_end_date = null;
        for (final Map.Entry<String, List<String>> match : matches.entrySet()) {
            final String key = match.getKey().toLowerCase();
            if (key.equals("id")) {
                shift_ids.addAll(match.getValue());
                //Only allow one date to query for from and to, if multiple ones appear on the query only use the first one
            } else if(key.equals("from")) {
                shift_start_date = match.getValue().iterator().next();
                //Only allow one date to query for from and to, if multiple ones appear on the query only use the first one
            } else if (key.equals("to")) {
                shift_end_date = match.getValue().iterator().next();
            } else if (key.equals("owner")) {
                shift_owners.addAll(match.getValue());
            } else if (key.equals("description")) {
                descriptions.addAll(match.getValue());
            } else if (key.equals("type")) {
                typeIds.addAll(findTypesIdByName(match.getValue().toArray(new String[match.getValue().size()])));
            } else if (key.equals("leadOperator")) {
                leadOperators.addAll(match.getValue());
            } else if (key.equals("onShiftPersonal")) {
                onShiftOperators.addAll(match.getValue());
            } else if (key.equals("closeUser")) {
                onShiftOperators.addAll(match.getValue());
            } else if (key.equals("status")) {
                status = match.getValue().iterator().next();
            }
        }
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        Join<Shift, Type> type = from.join(Shift_.type, JoinType.LEFT);
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
        if(!typeIds.isEmpty()) {
            typenPredicate = cb.or(type.get(Type_.id).in(typeIds), typenPredicate);
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
            closeUserPredicate = cb.or(from.get(Shift_.closeShiftUser).in(closeUsers), closeUserPredicate);
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
        Predicate statusPredicate = cb.disjunction();
        if(status != null) {
            if (status.equalsIgnoreCase("active")) {
                statusPredicate = cb.or(from.get(Shift_.endDate).isNull(), statusPredicate);
            } else if (status.equalsIgnoreCase("end")) {
                statusPredicate = cb.or(from.get(Shift_.endDate).isNotNull(), statusPredicate);
                closeUserPredicate = cb.or(from.get(Shift_.closeShiftUser).isNull(), closeUserPredicate);
            } else if (status.equalsIgnoreCase("close")) {
                statusPredicate = cb.or(from.get(Shift_.endDate).isNotNull(), statusPredicate);
                closeUserPredicate = cb.or(from.get(Shift_.closeShiftUser).isNotNull(), closeUserPredicate);
            }
        }
        final Predicate finalPredicate = cb.and(idPredicate, ownerPredicate, descriptionPredicate, typenPredicate,
                datePredicate, leadPredicate, onShiftPersonalPredicate, closeUserPredicate, statusPredicate);
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
            final Shift existingShift = findShiftById(shift.getId());
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
            final Shift existingShift = findShiftById(shift.getId());
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
    public Shift getOpenShift(final String typeName) throws ShiftFinderException {
        List<Integer> typeIds = findTypesIdByName(typeName);
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        Join<Shift, Type> type = from.join(Shift_.type, JoinType.LEFT);
        final CriteriaQuery<Shift> select = cq.select(from);
        final Predicate typePredicate = cb.equal(type.get(Type_.id), typeIds);
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
            shift.setType(findTypeByName(shift.getType().getName()));
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

    public Types listTypes() {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Type> cq = cb.createQuery(Type.class);
        final Root<Type> from = cq.from(Type.class);
        final CriteriaQuery<Type> select = cq.select(from);
        final TypedQuery<Type> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            final Types result = new Types();
            final List<Type> rs = typedQuery.getResultList();
            if (rs != null) {
                Iterator<Type> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    result.add(iterator.next());
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

    public List<Integer> findTypesIdByName(final String... names) {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Type> cq = cb.createQuery(Type.class);
        final Root<Type> from = cq.from(Type.class);
        final CriteriaQuery<Type> select = cq.select(from);
        Predicate typePredicate = cb.disjunction();
        typePredicate = cb.or(from.get(Type_.name).in(names), typePredicate);
        select.where(typePredicate);
        final TypedQuery<Type> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            final List<Integer> result = new ArrayList<Integer>();
            final List<Type> rs = typedQuery.getResultList();
            if (rs != null) {
                Iterator<Type> iterator = rs.iterator();
                while (iterator.hasNext()) {
                    result.add(iterator.next().getId());
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

    public Type findTypeByName(final String name) {
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Type> cq = cb.createQuery(Type.class);
        final Root<Type> from = cq.from(Type.class);
        final CriteriaQuery<Type> select = cq.select(from);
        Predicate typePredicate = cb.disjunction();
        typePredicate = cb.or(from.get(Type_.name).in(name), typePredicate);
        select.where(typePredicate);
        final TypedQuery<Type> typedQuery = em.createQuery(select);
        JPAUtil.startTransaction(em);
        try {
            final List<Integer> result = new ArrayList<Integer>();
            final List<Type> rs = typedQuery.getResultList();
            if (rs != null) {
                return rs.iterator().next();
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        } finally {
            JPAUtil.finishTransacton(em);
        }
    }
}
