package com.supermarket.dao;

import com.supermarket.entity.Supplier;
import com.supermarket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class SupplierDAO {

    public void save(Supplier entity) {
        inTransaction(session -> session.persist(entity));
    }

    public void update(Supplier entity) {
        inTransaction(session -> session.merge(entity));
    }

    public void delete(Long id) {
        inTransaction(session -> {
            Supplier found = session.find(Supplier.class, id);
            if (found != null) session.remove(found);
        });
    }

    public Supplier findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Supplier.class, id);
        }
    }

    public List<Supplier> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Supplier", Supplier.class).list();
        }
    }

    private void inTransaction(java.util.function.Consumer<Session> work) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            work.accept(session);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }
}
