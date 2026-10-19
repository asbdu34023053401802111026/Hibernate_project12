package com.supermarket;

import com.supermarket.dao.*;
import com.supermarket.entity.*;
import com.supermarket.util.HibernateUtil;

import java.math.BigDecimal;

public class App {

    public static void main(String[] args) {
        CategoryDAO categoryDAO = new CategoryDAO();
        SupplierDAO supplierDAO = new SupplierDAO();
        ProductDAO productDAO = new ProductDAO();
        CustomerDAO customerDAO = new CustomerDAO();
        OrderDAO orderDAO = new OrderDAO();

        try {
            Category dairy = new Category();
            dairy.setName("Dairy");
            dairy.setDescription("Milk, cheese and yogurt");
            categoryDAO.save(dairy);

            Supplier supplier = new Supplier();
            supplier.setName("Fresh Farms Ltd");
            supplier.setContactEmail("sales@freshfarms.example");
            supplier.setPhone("+91 90000 00000");
            supplierDAO.save(supplier);

            Product milk = new Product();
            milk.setName("Full Cream Milk 1L");
            milk.setPrice(new BigDecimal("2.50"));
            milk.setStockQuantity(100);
            milk.setCategory(dairy);
            milk.setSupplier(supplier);
            productDAO.save(milk);

            Customer customer = new Customer();
            customer.setName("Asha Kumar");
            customer.setEmail("asha@example.com");
            customer.setPhone("+91 98888 88888");
            customerDAO.save(customer);

            Order order = new Order();
            order.setCustomer(customer);
            OrderItem item = new OrderItem();
            item.setProduct(milk);
            item.setQuantity(4);
            order.addItem(item);
            orderDAO.placeOrder(order);

            Order saved = orderDAO.findById(order.getId());
            System.out.println("Placed " + saved + " total = " + saved.getTotalAmount());
            System.out.println("Milk stock left = " + productDAO.findById(milk.getId()).getStockQuantity());
            System.out.println("Low stock (<=10): " + productDAO.findLowStock(10));
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
