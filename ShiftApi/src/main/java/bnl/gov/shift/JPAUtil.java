package bnl.gov.shift;

import javax.persistence.*;

import org.apache.log4j.Logger;

public class JPAUtil {

    private static final EntityManagerFactory factory;
    private static volatile long aliasCount = 0;
    private static final Logger logger = Logger.getLogger(JPAUtil.class);

    static {
        try {
            factory = Persistence.createEntityManagerFactory("bnl/gov/shift");

        } catch (Throwable ex) {
            logger.error("Initial SessionFactory creation failed", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return factory;
    }

    public static void startTransaction(EntityManager em) {
        em.getTransaction().begin();
    }

    public static void finishTransacton(EntityManager em) {
        if (em.isOpen()) {
            EntityTransaction tx = em.getTransaction();
            if (tx.isActive()) {
                em.getTransaction().commit();
            }
            em.close();
        }
    }

    public static void transactionFailed(EntityManager em) {
        if (em.isOpen()) {
            EntityTransaction tx = em.getTransaction();

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
    }

    public static void save(Object o) {
        EntityManager em = null;

        try {
            em = JPAUtil.getEntityManagerFactory().createEntityManager();
            JPAUtil.startTransaction(em);
            em.persist(o);
            JPAUtil.finishTransacton(em);

        } catch (PersistenceException e) {
            JPAUtil.transactionFailed(em);
            throw e;
        }
    }

    public static Object update(Object o) {
        EntityManager em = null;

        try {
            em = JPAUtil.getEntityManagerFactory().createEntityManager();
            JPAUtil.startTransaction(em);
            o = em.merge(o);
            JPAUtil.finishTransacton(em);
            return o;

        } catch (PersistenceException e) {
            JPAUtil.transactionFailed(em);
            throw e;
        }
    }

    public static void remove(Class type, Long id) {
        EntityManager em = null;

        try {
            em = JPAUtil.getEntityManagerFactory().createEntityManager();
            JPAUtil.startTransaction(em);

            Query query = em.createQuery("UPDATE " + type.getName() + " c  SET c.state= edu.msu.nscl.olog.State.Inactive  WHERE c.id = " + id.toString());
            query.executeUpdate();

            JPAUtil.finishTransacton(em);

        } catch (PersistenceException e) {
            JPAUtil.transactionFailed(em);
            throw e;
        }
    }

}
