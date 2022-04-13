package gov.bnl.shift;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static gov.bnl.shift.ShiftResource.log;
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
        List<Predicate> andPredicates = new ArrayList<Predicate>();
        List<Predicate> orPredicates = new ArrayList<Predicate>();
        
        final List<String> shift_ids = new LinkedList<String>();
        final List<String> shift_owners = new LinkedList<String>();
        final List<String> descriptions = new LinkedList<String>();
        final List<String> leadOperators = new LinkedList<String>();
        final List<String> onShiftOperators = new LinkedList<String>();
        final List<Integer> typeIds = new LinkedList<Integer>();
        final List<String> closeUsers = new LinkedList<String>();
        final Multimap<String, String> paginate_matches = ArrayListMultimap.create();

        String status = null;
        String shift_start_date = null;
        String shift_end_date = null;
        
        for (final Map.Entry<String, List<String>> match : matches.entrySet()) {
            final String key = match.getKey().toLowerCase();
            if (key.equalsIgnoreCase("id")) {
                shift_ids.addAll(match.getValue());
                //Only allow one date to query for from and to, if multiple ones appear on the query only use the first one
            } else if(key.equalsIgnoreCase("from")) {
                shift_start_date = match.getValue().iterator().next();
                //Only allow one date to query for from and to, if multiple ones appear on the query only use the first one
            } else if (key.equalsIgnoreCase("to")) {
                shift_end_date = match.getValue().iterator().next();
            } else if (key.equalsIgnoreCase("owner")) {
                shift_owners.addAll(match.getValue());
            } else if (key.equalsIgnoreCase("description")) {
                descriptions.addAll(match.getValue());
            } else if (key.equalsIgnoreCase("type")) {
                typeIds.addAll(findTypesIdByName(match.getValue().toArray(new String[match.getValue().size()])));
            } else if (key.equalsIgnoreCase("leadoperator")) {
                leadOperators.addAll(match.getValue());
            } else if (key.equalsIgnoreCase("onshiftpersonal")) {
                onShiftOperators.addAll(match.getValue());
            } else if (key.equals("page")) {
                paginate_matches.putAll(key, match.getValue());
            } else if (key.equals("limit")) {
                paginate_matches.putAll(key, match.getValue());
            } else if (key.equalsIgnoreCase("closeuser")) {
                closeUsers.addAll(match.getValue());
            } else if (key.equalsIgnoreCase("status")) {
                status = match.getValue().iterator().next();
            }
        }
        em = JPAUtil.getEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Shift> cq = cb.createQuery(Shift.class);
        final Root<Shift> from = cq.from(Shift.class);
        Join<Shift, Type> type = from.join(Shift_.type, JoinType.LEFT);
        Predicate idPredicate = cb.disjunction();
        if (!shift_ids.isEmpty()) {
            idPredicate = cb.or(from.get(Shift_.id).in(shift_ids), idPredicate);
            andPredicates.add(idPredicate);
        }
        if(!shift_owners.isEmpty()) {
            Predicate ownerPredicate = cb.disjunction();
            for (String s : shift_owners) {
                ownerPredicate = cb.or(cb.equal(from.get(Shift_.owner), s), ownerPredicate);
            }
            andPredicates.add(ownerPredicate);
        }
        if(!descriptions.isEmpty()) {
            Predicate searchPredicate = cb.disjunction();
            for (String s : descriptions) {
                searchPredicate = cb.or(cb.like(from.get(Shift_.description), "%"+s+"%"), searchPredicate);
            }
            andPredicates.add(searchPredicate);
        }
        if(!typeIds.isEmpty()) {
            Predicate typenPredicate = cb.disjunction();
            typenPredicate = cb.or(type.get(Type_.id).in(typeIds), typenPredicate);
            andPredicates.add(typenPredicate);
        }
        if(!leadOperators.isEmpty()) {
            Predicate leadPredicate = cb.disjunction();
            leadPredicate = cb.or(from.get(Shift_.leadOperator).in(leadOperators), leadPredicate);
            andPredicates.add(leadPredicate);
        }
        if(!onShiftOperators.isEmpty()) {
            Predicate onShiftPersonalPredicate = cb.disjunction();
            onShiftPersonalPredicate = cb.or(from.get(Shift_.onShiftPersonal).in(onShiftOperators), onShiftPersonalPredicate);
            andPredicates.add(onShiftPersonalPredicate);
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
            andPredicates.add(datePredicate);
        }
        if(status != null) {
            Predicate statusPredicate = cb.disjunction();
            Predicate closeUserPredicate = cb.disjunction();
            if (status.equalsIgnoreCase("active")) {
                statusPredicate = cb.or(from.get(Shift_.endDate).isNull(), statusPredicate);
            } else if (status.equalsIgnoreCase("end")) {
                statusPredicate = cb.or(from.get(Shift_.endDate).isNotNull(), statusPredicate);
                closeUserPredicate = cb.or(from.get(Shift_.closeShiftUser).isNull(), closeUserPredicate);
                andPredicates.add(closeUserPredicate);
            } else if (status.equalsIgnoreCase("signed")) {
                statusPredicate = cb.or(from.get(Shift_.endDate).isNotNull(), statusPredicate);
                closeUserPredicate = cb.or(from.get(Shift_.closeShiftUser).isNotNull(), closeUserPredicate);
                andPredicates.add(closeUserPredicate);
            }
            andPredicates.add(statusPredicate);
        }
        
        Predicate finalPredicate = cb.conjunction();

        if (!andPredicates.isEmpty()) {
            Predicate andfinalPredicate = cb.conjunction();
            for (Predicate predicate : andPredicates) {
                andfinalPredicate = cb.and(andfinalPredicate, predicate);
            }
            finalPredicate = cb.and(finalPredicate, andfinalPredicate);
        }
        if (!orPredicates.isEmpty()) {
            Predicate orfinalPredicate = cb.disjunction();
            for (Predicate predicate : orPredicates) {
                orfinalPredicate = cb.or(orfinalPredicate, predicate);
            }
            finalPredicate = cb.and(finalPredicate, orfinalPredicate);
        }

        cq.where(finalPredicate);
        cq.groupBy(from);
        cq.distinct(true);
        cq.orderBy(cb.desc(from.get(Shift_.startDate)));
        final TypedQuery<Shift> typedQuery = em.createQuery(cq);
        if (!paginate_matches.isEmpty()) {
            String page = null, limit = null;
            for (Map.Entry<String, Collection<String>> match : paginate_matches.asMap().entrySet()) {
                if (match.getKey().toLowerCase().equals("limit")) {
                    limit = match.getValue().iterator().next();
                }
                if (match.getKey().toLowerCase().equals("page")) {
                    page = match.getValue().iterator().next();
                }
            }
            if (limit != null && page != null) {
                Integer offset = Integer.valueOf(page) * Integer.valueOf(limit) - Integer.valueOf(limit);
                typedQuery.setFirstResult(offset);
                typedQuery.setMaxResults(Integer.valueOf(limit));
            } else if (limit != null) {
                typedQuery.setMaxResults(Integer.valueOf(limit));
            } else {
                typedQuery.setMaxResults(Integer.valueOf(500));
            }
        }

        if(matches.isEmpty()) {
            return new Shifts();
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
            log.info("matches criteria " + matches.entrySet().stream().map(e -> {
                return e.getKey()+":"+String.join("", e.getValue());
            }).collect(Collectors.joining()) + "  result: " + result.size());
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                    "JPA exception: " + e);
        } finally {
            try {
                if (em.getTransaction() != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception e) {
            }
            em.close();
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
                    "User '" + um.getUserName() + "' does not belong to owner group '" + shift.getOwner() + "'");
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
            if(existingShift.getEndDate() != null)  {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                        "The shift " + existingShift.getId() + " is already end");
            }
            //Prevent that the user override this values
            shift.setStartDate(existingShift.getStartDate());
            shift.setOwner(existingShift.getOwner());
            shift.setLeadOperator(existingShift.getLeadOperator());
            shift.setType(existingShift.getType());
            shift.setEndDate(new Date());
            JPAUtil.update(shift);
            return shift;
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
            if(existingShift.getEndDate() == null || existingShift.getCloseShiftUser() != null)  {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR,
                        "The shift " + existingShift.getId() + " is already close");
            }
            //Prevent that the user override this values
            shift.setStartDate(existingShift.getStartDate());
            shift.setEndDate(existingShift.getEndDate());
            shift.setOwner(existingShift.getOwner());
            shift.setLeadOperator(existingShift.getLeadOperator());
            shift.setType(existingShift.getType());
            shift.setCloseShiftUser(user);
            JPAUtil.update(shift);
            return shift;
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
        cq.orderBy(cb.desc(from.get(Shift_.startDate)));
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
