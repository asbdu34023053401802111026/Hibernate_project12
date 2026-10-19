package com.supermarket.dao;

import com.supermarket.entity.Customer;
import com.supermarket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CustomerDAO {

    public void save(Customer entity) {
        inTransaction(session -> session.persist(entity));
    }

    public void update(Customer entity) {
        inTransaction(session -> session.merge(entity));
    }

    public void delete(Long id) {
        inTransaction(session -> {
            Customer found = session.find(Customer.class, id);
            if (found != null) session.remove(found);
        });
    }

    public Customer findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Customer.class, id);
        }
    }

    public List<Customer> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Customer", Customer.class).list();
        }
    }

    public Customer findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Customer c where c.email = :email", Customer.class)
                    .setParameter("email", email).uniqueResult();
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
