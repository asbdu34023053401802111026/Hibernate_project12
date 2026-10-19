package com.supermarket.dao;

import com.supermarket.entity.Category;
import com.supermarket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CategoryDAO {

    public void save(Category entity) {
        inTransaction(session -> session.persist(entity));
    }

    public void update(Category entity) {
        inTransaction(session -> session.merge(entity));
    }

    public void delete(Long id) {
        inTransaction(session -> {
            Category found = session.find(Category.class, id);
            if (found != null) session.remove(found);
        });
    }

    public Category findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Category.class, id);
        }
    }

    public List<Category> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Category", Category.class).list();
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
