package com.example.tryme.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.tryme.Model.Meal;
import com.example.tryme.Model.MealProduct;
import com.example.tryme.Model.Product;
import com.example.tryme.Repository.MealProductRepository; 
import com.example.tryme.exception.ResourceNotFoundException;

@Service
public class MealProductService {
    private final MealProductRepository mealProductRepository;
    private final CacheService cacheService;

    @Autowired
    public MealProductService(MealProductRepository mealProductRepository, CacheService cacheService) {
        this.mealProductRepository = mealProductRepository;
        this.cacheService = cacheService;
    }

  
    public String createMealProduct(Integer grams, Long mealId, Long productId,
                                    MealService mealService, ProductService productService) {
        cacheService.clearCache("mealProducts");
        
       
        Meal meal = mealService.getMeal(mealId);
        Product product = productService.getProduct(productId);
        
        MealProduct mealProduct = new MealProduct(grams, meal, product);
        mealProductRepository.save(mealProduct);
        return "MealProduct created with ID: " + mealProduct.getId();
    }

    public MealProduct getMealProduct(Long id) {
        String cacheKey = "id:" + id;
        List<MealProduct> cachedMealProductsList = cacheService.getFromCache("mealProducts", cacheKey);
        if (cachedMealProductsList != null && !cachedMealProductsList.isEmpty()) {
            return cachedMealProductsList.get(0);
        }
        MealProduct mealProduct = mealProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MealProduct not found with id: " + id));
        cacheService.putToCache("mealProducts", cacheKey, List.of(mealProduct));
        return mealProduct;
    }

    public String updateMealProduct(Long id, Integer grams) {
        cacheService.clearCache("mealProducts");
        MealProduct mealProduct = mealProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MealProduct not found with id: " + id + " for update."));
        mealProduct.setGrams(grams);
        mealProductRepository.save(mealProduct);
        return "MealProduct updated";
    }

    public String deleteMealProduct(Long id) {
        cacheService.clearCache("mealProducts");
        MealProduct mealProduct = mealProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MealProduct not found with id: " + id + " for deletion."));
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