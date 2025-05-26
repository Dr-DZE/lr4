package com.example.tryme.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tryme.Model.MealProduct;
import com.example.tryme.Repository.MealProductRepository;

@Service
public class MealProductService {
    private final MealProductRepository mealProductRepository;
    private final CacheService cacheService;

    public MealProductService(MealProductRepository mealProductRepository, CacheService cacheService) {
        this.mealProductRepository = mealProductRepository;
        this.cacheService = cacheService;
    }

    public String createMealProduct(Integer grams, Long mealId, Long productId, 
                                  MealService mealService, ProductService productService) {
        cacheService.clearCache("mealProducts");
        var meal = mealService.getMeal(mealId);
        var product = productService.getProduct(productId);
        
        MealProduct mealProduct = new MealProduct(grams, meal, product);
        mealProductRepository.save(mealProduct);
        return "MealProduct created with ID: " + mealProduct.getId();
    }

    public MealProduct getMealProduct(Long id) {
        List<MealProduct> cachedMealProducts = cacheService.getFromCache("mealProducts", "id:" + id);
        if (cachedMealProducts != null && !cachedMealProducts.isEmpty()) {
            return cachedMealProducts.get(0);
        }
        MealProduct mealProduct = mealProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MealProduct not found"));
        cacheService.putToCache("mealProducts", "id:" + id, List.of(mealProduct));
        return mealProduct;
    }

    public String updateMealProduct(Long id, Integer grams) {
        cacheService.clearCache("mealProducts");
        MealProduct mealProduct = mealProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MealProduct not found"));
        mealProduct.setGrams(grams);
        mealProductRepository.save(mealProduct);
        return "MealProduct updated";
    }

    public String deleteMealProduct(Long id) {
        cacheService.clearCache("mealProducts");
        MealProduct mealProduct = mealProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MealProduct not found"));
        mealProductRepository.delete(mealProduct);
        return "MealProduct deleted";
    }

    public List<MealProduct> getAllMealProducts() {
        List<MealProduct> cachedMealProducts = cacheService.getFromCache("mealProducts", "all");
        if (cachedMealProducts != null) {
            return cachedMealProducts;
        }
        List<MealProduct> mealProducts = mealProductRepository.findAll();
        cacheService.putToCache("mealProducts", "all", mealProducts);
        return mealProducts;
    }
}