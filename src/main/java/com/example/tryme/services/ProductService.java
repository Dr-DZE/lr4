package com.example.tryme.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tryme.Model.Product;
import com.example.tryme.Repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CacheService cacheService;

    public ProductService(ProductRepository productRepository, CacheService cacheService) {
        this.productRepository = productRepository;
        this.cacheService = cacheService;
    }

    public String createProduct(String name, Integer caloriesPer100g) {
        cacheService.clearCache("products");
        Product product = new Product(name, caloriesPer100g);
        productRepository.save(product);
        return "Product created with ID: " + product.getId();
    }

    public Product getProduct(Long id) {
        List<Product> cachedProducts = cacheService.getFromCache("products", "id:" + id);
        if (cachedProducts != null && !cachedProducts.isEmpty()) {
            return cachedProducts.get(0);
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        cacheService.putToCache("products", "id:" + id, List.of(product));
        return product;
    }

    public String updateProduct(Long id, String name, Integer caloriesPer100g) {
        cacheService.clearCache("products");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setName(name);
        product.setCaloriesPer100g(caloriesPer100g);
        productRepository.save(product);
        return "Product updated";
    }

    public String deleteProduct(Long id) {
        cacheService.clearCache("products");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productRepository.delete(product);
        return "Product deleted";
    }

    public List<Product> getAllProducts() {
        List<Product> cachedProducts = cacheService.getFromCache("products", "all");
        if (cachedProducts != null) {
            return cachedProducts;
        }
        List<Product> products = productRepository.findAll();
        cacheService.putToCache("products", "all", products);
        return products;
    }

    public List<Product> findByNameContainingIgnoreCase(String name) {
        List<Product> cachedProducts = cacheService.getFromCache("products", "name:" + name);
        if (cachedProducts != null) {
            return cachedProducts;
        }
        List<Product> products = productRepository.findByNameContainingIgnoreCase(name);
        cacheService.putToCache("products", "name:" + name, products);
        return products;
    }
}