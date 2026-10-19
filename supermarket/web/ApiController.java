package com.supermarket.web;

import com.supermarket.dao.*;
import com.supermarket.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CategoryDAO categories = new CategoryDAO();
    private final SupplierDAO suppliers = new SupplierDAO();
    private final ProductDAO products = new ProductDAO();
    private final CustomerDAO customers = new CustomerDAO();
    private final OrderDAO orders = new OrderDAO();

    // DTOs keep lazy Hibernate relations out of the JSON
    record CategoryDto(Long id, String name, String description) {}
    record SupplierDto(Long id, String name, String contactEmail, String phone) {}
    record CustomerDto(Long id, String name, String email, String phone) {}
    record ProductReq(String name, BigDecimal price, Integer stockQuantity, Long categoryId, Long supplierId) {}
    record ProductDto(Long id, String name, BigDecimal price, Integer stockQuantity,
                      Long categoryId, String categoryName, Long supplierId, String supplierName) {}
    record ItemReq(Long productId, Integer quantity) {}
    record OrderReq(Long customerId, List<ItemReq> items) {}
    record ItemDto(String productName, Integer quantity, BigDecimal unitPrice) {}
    record OrderDto(Long id, LocalDateTime orderDate, BigDecimal totalAmount, String customerName, List<ItemDto> items) {}

    private static <T> T found(T value) {
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        return value;
    }

    // ---- Categories ----
    private CategoryDto dto(Category c) { return new CategoryDto(c.getId(), c.getName(), c.getDescription()); }
    @GetMapping("/categories") List<CategoryDto> listCategories() { return categories.findAll().stream().map(this::dto).toList(); }
    @PostMapping("/categories") CategoryDto addCategory(@RequestBody CategoryDto d) {
        Category c = new Category(); c.setName(d.name()); c.setDescription(d.description());
        categories.save(c); return dto(c);
    }
    @PutMapping("/categories/{id}") CategoryDto editCategory(@PathVariable Long id, @RequestBody CategoryDto d) {
        Category c = found(categories.findById(id)); c.setName(d.name()); c.setDescription(d.description());
        categories.update(c); return dto(c);
    }
    @DeleteMapping("/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCategory(@PathVariable Long id) { categories.delete(id); }

    // ---- Suppliers ----
    private SupplierDto dto(Supplier s) { return new SupplierDto(s.getId(), s.getName(), s.getContactEmail(), s.getPhone()); }
    @GetMapping("/suppliers") List<SupplierDto> listSuppliers() { return suppliers.findAll().stream().map(this::dto).toList(); }
    @PostMapping("/suppliers") SupplierDto addSupplier(@RequestBody SupplierDto d) {
        Supplier s = new Supplier(); s.setName(d.name()); s.setContactEmail(d.contactEmail()); s.setPhone(d.phone());
        suppliers.save(s); return dto(s);
    }
    @PutMapping("/suppliers/{id}") SupplierDto editSupplier(@PathVariable Long id, @RequestBody SupplierDto d) {
        Supplier s = found(suppliers.findById(id)); s.setName(d.name()); s.setContactEmail(d.contactEmail()); s.setPhone(d.phone());
        suppliers.update(s); return dto(s);
    }
    @DeleteMapping("/suppliers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteSupplier(@PathVariable Long id) { suppliers.delete(id); }

    // ---- Customers ----
    private CustomerDto dto(Customer c) { return new CustomerDto(c.getId(), c.getName(), c.getEmail(), c.getPhone()); }
    @GetMapping("/customers") List<CustomerDto> listCustomers() { return customers.findAll().stream().map(this::dto).toList(); }
    @PostMapping("/customers") CustomerDto addCustomer(@RequestBody CustomerDto d) {
        Customer c = new Customer(); c.setName(d.name()); c.setEmail(d.email()); c.setPhone(d.phone());
        customers.save(c); return dto(c);
    }
    @PutMapping("/customers/{id}") CustomerDto editCustomer(@PathVariable Long id, @RequestBody CustomerDto d) {
        Customer c = found(customers.findById(id)); c.setName(d.name()); c.setEmail(d.email()); c.setPhone(d.phone());
        customers.update(c); return dto(c);
    }
    @DeleteMapping("/customers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCustomer(@PathVariable Long id) { customers.delete(id); }

    // ---- Products ----
    private ProductDto dto(Product p) {
        Category c = p.getCategory(); Supplier s = p.getSupplier();
        return new ProductDto(p.getId(), p.getName(), p.getPrice(), p.getStockQuantity(),
                c == null ? null : c.getId(), c == null ? null : c.getName(),
                s == null ? null : s.getId(), s == null ? null : s.getName());
    }
    private void fill(Product p, ProductReq r) {
        p.setName(r.name()); p.setPrice(r.price()); p.setStockQuantity(r.stockQuantity());
        p.setCategory(r.categoryId() == null ? null : found(categories.findById(r.categoryId())));
        p.setSupplier(r.supplierId() == null ? null : found(suppliers.findById(r.supplierId())));
    }
    @GetMapping("/products") List<ProductDto> listProducts() { return products.findAll().stream().map(this::dto).toList(); }
    @PostMapping("/products") ProductDto addProduct(@RequestBody ProductReq r) {
        Product p = new Product(); fill(p, r); products.save(p); return dto(p);
    }
    @PutMapping("/products/{id}") ProductDto editProduct(@PathVariable Long id, @RequestBody ProductReq r) {
        Product p = new Product(); p.setId(found(products.findById(id)).getId()); fill(p, r);
        products.update(p); return dto(p);
    }
    @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteProduct(@PathVariable Long id) { products.delete(id); }

    // ---- Orders ----
    private OrderDto dto(Order o) {
        return new OrderDto(o.getId(), o.getOrderDate(), o.getTotalAmount(), o.getCustomer().getName(),
                o.getItems().stream().map(i -> new ItemDto(i.getProduct().getName(), i.getQuantity(), i.getUnitPrice())).toList());
    }
    @GetMapping("/orders") List<OrderDto> listOrders() { return orders.findAll().stream().map(this::dto).toList(); }
    @PostMapping("/orders") OrderDto placeOrder(@RequestBody OrderReq r) {
        if (r.items() == null || r.items().isEmpty()) throw new IllegalArgumentException("Add at least one item");
        Order o = new Order();
        o.setCustomer(found(customers.findById(r.customerId())));
        for (ItemReq i : r.items()) {
            if (i.quantity() == null || i.quantity() < 1) throw new IllegalArgumentException("Quantity must be at least 1");
            Product p = new Product(); p.setId(i.productId());
            OrderItem item = new OrderItem(); item.setProduct(p); item.setQuantity(i.quantity());
            o.addItem(item);
        }
        orders.placeOrder(o);
        return dto(orders.findById(o.getId()));
    }
    @DeleteMapping("/orders/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteOrder(@PathVariable Long id) { orders.delete(id); }

    // ---- Errors ----
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason())));
    }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, String>> conflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Could not save. The name or email may already exist, or the record is in use."));
    }
}
