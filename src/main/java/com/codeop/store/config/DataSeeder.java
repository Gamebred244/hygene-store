package com.codeop.store.config;

import com.codeop.store.model.Product;
import com.codeop.store.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> products = List.of(
                build("Aloe Vera Shampoo", "Gentle daily shampoo with aloe for soft, clean hair.",
                        "SHAM-ALOE-250", "Hair Care", "https://images.unsplash.com/photo-1585238342028-4abc3bbfd4b7?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("8.90")),
                build("Fresh Mint Soap", "Refreshing bar soap with mint oil for a clean finish.",
                        "SOAP-MINT-100", "Body Care", "https://images.unsplash.com/photo-1584305574647-0a7f02da3c0e?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("2.40")),
                build("Hydrating Body Lotion", "Lightweight lotion for dry skin with shea butter.",
                        "LOT-SHEA-300", "Body Care", "https://images.unsplash.com/photo-1585386959984-a4155228f47e?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("9.50")),
                build("Sensitive Toothpaste", "Low-foam toothpaste for sensitive teeth and gums.",
                        "TP-SENS-120", "Oral Care", "https://images.unsplash.com/photo-1588776814546-1ffcf47267b5?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("4.20")),
                build("Soft Toothbrush Pack", "Pack of 3 ultra-soft toothbrushes for daily use.",
                        "TB-SOFT-3PK", "Oral Care", "https://images.unsplash.com/photo-1588776813677-77c43a39b8e5?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("3.80")),
                build("Gentle Face Wash", "Soap-free face wash with chamomile for sensitive skin.",
                        "FACE-CHAM-150", "Face Care", "https://images.unsplash.com/photo-1588514727390-8b0a0f7a2d2a?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("7.30")),
                build("Citrus Hand Wash", "Foaming hand wash with citrus scent.",
                        "HAND-CIT-250", "Hand Care", "https://images.unsplash.com/photo-1584983733700-8be2d7d9ef86?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("3.60")),
                build("Daily Deodorant", "Long-lasting deodorant with clean cotton scent.",
                        "DEO-COT-75", "Body Care", "https://images.unsplash.com/photo-1588776814547-1a1c8b5f9f67?auto=format&fit=crop&w=600&q=80",
                        new BigDecimal("5.40"))
        );

        productRepository.saveAll(products);
    }

    private Product build(String name, String description, String sku, String category, String imageUrl,
                          BigDecimal price) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setSku(sku);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setPrice(price);
        product.setCurrency("USD");
        product.setStockQuantity(100);
        product.setActive(true);
        return product;
    }
}
