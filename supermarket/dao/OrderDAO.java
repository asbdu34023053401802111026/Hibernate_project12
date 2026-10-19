package com.supermarket.dao;

import com.supermarket.entity.Order;
import com.supermarket.entity.OrderItem;
import com.supermarket.entity.Product;
import com.supermarket.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDAO {

    /**
     * Saves an order, checks stock for every item, reduces stock and calculates the total.
     * Everything runs in one transaction, so a failure leaves the database unchanged.
     */
    public void placeOrder(Order order) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItem item : order.getItems()) {
                Product product = session.find(Product.class, item.getProduct().getId());
                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + item.getProduct().getId());
                }
                if (product.getStockQuantity() < item.getQuantity()) {
                    throw new IllegalStateException("Not enough stock for " + product.getName());
                }
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                item.setProduct(product);
                item.setUnitPrice(product.getPrice());
                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(total);
            session.persist(order);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    public Order findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select distinct o from Order o join fetch o.customer left join fetch o.items i left join fetch i.product where o.id = :id", Order.class)
                    .setParameter("id", id).uniqueResult();
        }
    }

    public List<Order> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("select distinct o from Order o join fetch o.customer left join fetch o.items i left join fetch i.product order by o.orderDate desc", Order.class).list();
        }
    }

    public List<Order> findByCustomer(Long customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select distinct o from Order o join fetch o.customer left join fetch o.items i left join fetch i.product where o.customer.id = :cid order by o.orderDate desc", Order.class)
                    .setParameter("cid", customerId).list();
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Order found = session.find(Order.class, id);
            if (found != null) session.remove(found);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }
}
