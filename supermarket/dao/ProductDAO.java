package com.supermarket.dao;

import com.supermarket.entity.Product;
import com.supermarket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ProductDAO {

    public void save(Product entity) {
        inTransaction(session -> session.persist(entity));
    }

    public void update(Product entity) {
        inTransaction(session -> session.merge(entity));
    }

    public void delete(Long id) {
        inTransaction(session -> {
            Product found = session.find(Product.class, id);
            if (found != null) session.remove(found);
        });
    }

    public Product findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Product.class, id);
        }
    }

    public List<Product> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("select p from Product p left join fetch p.category left join fetch p.supplier order by p.name", Product.class).list();
        }
    }

    public List<Product> findByCategory(Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("select p from Product p left join fetch p.category left join fetch p.supplier where p.category.id = :cid", Product.class)
                    .setParameter("cid", categoryId).list();
        }
    }

    public List<Product> findLowStock(int threshold) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("select p from Product p left join fetch p.category left join fetch p.supplier where p.stockQuantity <= :t order by p.stockQuantity", Product.class)
                    .setParameter("t", threshold).list();
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
