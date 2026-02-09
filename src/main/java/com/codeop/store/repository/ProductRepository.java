package com.codeop.store.repository;

import com.codeop.store.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name,
            String description,
            String category,
            String sku
    );
}
